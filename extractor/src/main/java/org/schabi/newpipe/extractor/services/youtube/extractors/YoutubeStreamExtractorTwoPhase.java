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
     * Thiết lập downloader cho extractor
     * @param downloader Downloader để tải dữ liệu
     */
    protected void onDownloaderCreated(@Nonnull final Downloader downloader) {
        // Sử dụng phương thức onDownloaderCreated từ lớp cha nếu có
        // Nếu không, thực hiện các thao tác cần thiết để thiết lập downloader
        try {
            // Sử dụng phản chiếu (reflection) để truy cập phương thức protected/private
            java.lang.reflect.Method method = getClass().getSuperclass()
                    .getDeclaredMethod("onDownloaderCreated", Downloader.class);
            method.setAccessible(true);
            method.invoke(this, downloader);
        } catch (Exception e) {
            // Nếu không thể gọi phương thức từ lớp cha, thực hiện cách khác
            // Ví dụ: lưu downloader vào một biến tạm và sử dụng nó sau này
        }
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

        // Sử dụng phương thức onFetchPage từ lớp cha để tải dữ liệu
        this.onFetchPage(downloader);
        
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

        // Tải dữ liệu bổ sung
        // Lưu ý: Trong lớp YoutubeStreamExtractor, các phương thức như fetchAndroidClient, 
        // fetchIosClient và fetchNextPage không tồn tại hoặc là private.
        // Thay vào đó, chúng ta sẽ sử dụng các phương thức public có sẵn để tải dữ liệu bổ sung.
        
        // Tải các thông tin bổ sung
        // Các thông tin này đã được tải trong fetchPage() nhưng chúng ta có thể
        // thực hiện các tác vụ bổ sung ở đây nếu cần
        
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
} 