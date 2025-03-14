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

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

/**
 * Lớp trích xuất YouTube với khả năng tải dữ liệu theo 2 giai đoạn.
 * Giai đoạn 1: Tải dữ liệu cần thiết để phát video ngay lập tức
 * Giai đoạn 2: Tải dữ liệu bổ sung dưới nền
 */
public class YoutubeStreamExtractorTwoPhase extends YoutubeStreamExtractor {
    private boolean essentialDataLoaded = false;
    private boolean additionalDataLoaded = false;

    public YoutubeStreamExtractorTwoPhase(final StreamingService service,
                                         final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    /**
     * Tải dữ liệu cần thiết (giai đoạn 1) để phát video ngay lập tức
     * @param downloader Downloader để tải dữ liệu
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public void fetchEssentialData(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        if (essentialDataLoaded) {
            return;
        }

        this.downloader = downloader;
        
        // Chỉ tải dữ liệu từ client HTML5 để có được URL phát nhanh nhất
        fetchHtml5Client(getLocalization(), getContentCountry(), getId(), 
                getPoTokenproviderInstance(), isNoPoTokenProviderSet());
        
        setStreamType();
        
        essentialDataLoaded = true;
    }
    
    /**
     * Tải dữ liệu bổ sung (giai đoạn 2) dưới nền
     * @param downloader Downloader để tải dữ liệu
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public void fetchAdditionalData(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        if (additionalDataLoaded) {
            return;
        }

        if (!essentialDataLoaded) {
            fetchEssentialData(downloader);
        }

        this.downloader = downloader;
        
        // Tải dữ liệu từ client Android và iOS
        fetchAndroidClient(getLocalization(), getContentCountry(), getId());
        fetchIosClient(getLocalization(), getContentCountry(), getId());
        
        // Tải dữ liệu "next" (video liên quan)
        fetchNextPage();
        
        additionalDataLoaded = true;
    }
    
    /**
     * Lấy danh sách các luồng video cần thiết cho việc phát ngay lập tức
     * @return Danh sách các luồng video
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public List<VideoStream> getEssentialVideoStreams() throws ExtractionException {
        if (!essentialDataLoaded) {
            throw new ExtractionException("Essential data not loaded yet");
        }
        return getVideoStreams();
    }
    
    /**
     * Lấy danh sách các luồng âm thanh cần thiết cho việc phát ngay lập tức
     * @return Danh sách các luồng âm thanh
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public List<AudioStream> getEssentialAudioStreams() throws ExtractionException {
        if (!essentialDataLoaded) {
            throw new ExtractionException("Essential data not loaded yet");
        }
        return getAudioStreams();
    }

    /**
     * Kiểm tra xem dữ liệu cần thiết đã được tải chưa
     * @return true nếu dữ liệu cần thiết đã được tải
     */
    public boolean isEssentialDataLoaded() {
        return essentialDataLoaded;
    }

    /**
     * Kiểm tra xem dữ liệu bổ sung đã được tải chưa
     * @return true nếu dữ liệu bổ sung đã được tải
     */
    public boolean isAdditionalDataLoaded() {
        return additionalDataLoaded;
    }

    // Các phương thức trợ giúp để truy cập các thuộc tính protected từ lớp cha
    private Localization getLocalization() {
        return localization;
    }

    private ContentCountry getContentCountry() {
        return contentCountry;
    }

    private String getId() {
        return videoId;
    }

    private PoTokenProvider getPoTokenproviderInstance() {
        return poTokenproviderInstance;
    }

    private boolean isNoPoTokenProviderSet() {
        return noPoTokenProviderSet;
    }
} 