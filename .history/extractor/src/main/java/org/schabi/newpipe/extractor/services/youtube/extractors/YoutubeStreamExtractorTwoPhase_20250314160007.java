/*
 * Created by WincaTubeExtractor on 14.03.2024.
 *
 * Copyright (C) 2024 WincaTubeExtractor
 * YoutubeStreamExtractorTwoPhase.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.extractors;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.VIDEO_ID;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider;
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Lớp trích xuất YouTube với cơ chế tải 2 giai đoạn.
 * Giai đoạn 1: Tải thông tin cần thiết để phát video ngay lập tức.
 * Giai đoạn 2: Tải thông tin bổ sung dưới nền sau khi video đã bắt đầu phát.
 */
public class YoutubeStreamExtractorTwoPhase extends YoutubeStreamExtractor {
    private static final String NEXT = "next";
    
    private boolean essentialDataLoaded = false;
    private boolean additionalDataLoaded = false;
    
    public YoutubeStreamExtractorTwoPhase(final StreamingService service, 
                                         final LinkHandler linkHandler) {
        super(service, linkHandler);
    }
    
    /**
     * Tải các thông tin cần thiết để phát video ngay lập tức (giai đoạn 1).
     * Phương thức này chỉ tải dữ liệu từ client HTML5 để có được URL phát nhanh nhất.
     *
     * @param downloader Downloader để tải dữ liệu
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public void fetchEssentialData(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        if (essentialDataLoaded) {
            return;
        }
        
        // Chỉ tải dữ liệu từ client HTML5 để có được URL phát nhanh nhất
        fetchHtml5Client(localization, contentCountry, videoId, poTokenproviderInstance,
                noPoTokenProviderSet);
        
        setStreamType();
        
        essentialDataLoaded = true;
    }
    
    /**
     * Tải các thông tin bổ sung dưới nền (giai đoạn 2).
     * Phương thức này nên được gọi sau khi gọi fetchEssentialData() và video đã bắt đầu phát.
     *
     * @param downloader Downloader để tải dữ liệu
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public void fetchAdditionalData(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        if (!essentialDataLoaded) {
            throw new ExtractionException("Essential data must be loaded before additional data");
        }
        
        if (additionalDataLoaded) {
            return;
        }
        
        // Tải dữ liệu từ client Android và iOS
        final PoTokenResult androidPoTokenResult = noPoTokenProviderSet ? null
                : poTokenproviderInstance.getAndroidClientPoToken(videoId);
        fetchAndroidClient(localization, contentCountry, videoId, androidPoTokenResult);
        
        // Tải dữ liệu từ client iOS nếu cần
        if (fetchIosClient) {
            final PoTokenResult iosPoTokenResult = noPoTokenProviderSet ? null
                    : poTokenproviderInstance.getIosClientPoToken(videoId);
            fetchIosClient(localization, contentCountry, videoId, iosPoTokenResult);
        }
        
        // Tải dữ liệu "next" (video liên quan)
        fetchNextResponse(localization, contentCountry, videoId);
        
        additionalDataLoaded = true;
    }
    
    /**
     * Tải dữ liệu "next" chứa thông tin về video liên quan và các thông tin bổ sung khác.
     *
     * @param localization Thông tin ngôn ngữ
     * @param contentCountry Thông tin quốc gia
     * @param videoId ID của video
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    private void fetchNextResponse(@Nonnull final Localization localization,
                                  @Nonnull final ContentCountry contentCountry,
                                  @Nonnull final String videoId) 
            throws IOException, ExtractionException {
        try {
            final byte[] nextBody = JsonWriter.string(
                    prepareDesktopJsonBuilder(localization, contentCountry)
                            .value(VIDEO_ID, videoId)
                            .value(CONTENT_CHECK_OK, true)
                            .value(RACY_CHECK_OK, true)
                            .done())
                    .getBytes(StandardCharsets.UTF_8);
            nextResponse = getJsonPostResponse(NEXT, nextBody, localization);
        } catch (final Exception e) {
            // Bỏ qua lỗi khi tải dữ liệu "next" vì nó không cần thiết để phát video
        }
    }
    
    /**
     * Lấy danh sách các luồng video cần thiết đã được tải trong giai đoạn 1.
     *
     * @return Danh sách các luồng video cần thiết
     * @throws ParsingException nếu có lỗi khi phân tích dữ liệu
     */
    public List<VideoStream> getEssentialVideoStreams() throws ParsingException {
        if (!essentialDataLoaded) {
            throw new ParsingException("Essential data must be loaded first");
        }
        return getVideoStreams();
    }
    
    /**
     * Lấy danh sách các luồng âm thanh cần thiết đã được tải trong giai đoạn 1.
     *
     * @return Danh sách các luồng âm thanh cần thiết
     * @throws ParsingException nếu có lỗi khi phân tích dữ liệu
     */
    public List<AudioStream> getEssentialAudioStreams() throws ParsingException {
        if (!essentialDataLoaded) {
            throw new ParsingException("Essential data must be loaded first");
        }
        return getAudioStreams();
    }
    
    /**
     * Kiểm tra xem dữ liệu thiết yếu đã được tải hay chưa.
     *
     * @return true nếu dữ liệu thiết yếu đã được tải, false nếu chưa
     */
    public boolean isEssentialDataLoaded() {
        return essentialDataLoaded;
    }
    
    /**
     * Kiểm tra xem dữ liệu bổ sung đã được tải hay chưa.
     *
     * @return true nếu dữ liệu bổ sung đã được tải, false nếu chưa
     */
    public boolean isAdditionalDataLoaded() {
        return additionalDataLoaded;
    }
} 