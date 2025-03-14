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

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * Lớp trích xuất YouTube với cơ chế tải 2 giai đoạn.
 * Giai đoạn 1: Tải thông tin cần thiết để phát video ngay lập tức.
 * Giai đoạn 2: Tải thông tin bổ sung dưới nền sau khi video đã bắt đầu phát.
 */
public class YoutubeStreamExtractorTwoPhase extends YoutubeStreamExtractor {
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
        fetchAndroidClient(localization, contentCountry, videoId);
        fetchIosClient(localization, contentCountry, videoId);
        
        // Tải dữ liệu "next" (video liên quan)
        fetchNextResponse(localization, contentCountry, videoId);
        
        additionalDataLoaded = true;
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