/*
 * Created by WincaTubeExtractor on 14.03.2024.
 *
 * Copyright (C) 2024 WincaTubeExtractor
 * StreamInfoTwoPhase.java is part of NewPipe Extractor.
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

package org.schabi.newpipe.extractor.stream;

import org.schabi.newpipe.extractor.Info;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractorTwoPhase;

import java.io.IOException;

import javax.annotation.Nonnull;

/**
 * Lớp thông tin cho nội dung đã mở, tức là nội dung sẵn sàng để phát,
 * với cơ chế tải 2 giai đoạn.
 */
public class StreamInfoTwoPhase extends StreamInfo {

    private final YoutubeStreamExtractorTwoPhase extractor;
    private boolean additionalDataLoaded = false;

    private StreamInfoTwoPhase(final int serviceId,
                              final String url,
                              final String originalUrl,
                              final StreamType streamType,
                              final String id,
                              final String name,
                              final int ageLimit,
                              final YoutubeStreamExtractorTwoPhase extractor) {
        super(serviceId, url, originalUrl, streamType, id, name, ageLimit);
        this.extractor = extractor;
    }

    /**
     * Lấy thông tin stream với cơ chế tải 2 giai đoạn.
     * Phương thức này chỉ tải dữ liệu cần thiết để phát video ngay lập tức.
     *
     * @param url URL của video
     * @return Đối tượng StreamInfoTwoPhase chứa thông tin cần thiết để phát video
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public static StreamInfoTwoPhase getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(NewPipe.getServiceByUrl(url), url);
    }

    /**
     * Lấy thông tin stream với cơ chế tải 2 giai đoạn.
     * Phương thức này chỉ tải dữ liệu cần thiết để phát video ngay lập tức.
     *
     * @param service Dịch vụ phát trực tuyến
     * @param url URL của video
     * @return Đối tượng StreamInfoTwoPhase chứa thông tin cần thiết để phát video
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public static StreamInfoTwoPhase getInfo(@Nonnull final StreamingService service,
                                           final String url) throws IOException, ExtractionException {
        final YoutubeStreamExtractorTwoPhase extractor = 
                (YoutubeStreamExtractorTwoPhase) service.getStreamExtractor(url);
        return getInfo(extractor);
    }

    /**
     * Lấy thông tin stream với cơ chế tải 2 giai đoạn.
     * Phương thức này chỉ tải dữ liệu cần thiết để phát video ngay lập tức.
     *
     * @param extractor Trình trích xuất stream
     * @return Đối tượng StreamInfoTwoPhase chứa thông tin cần thiết để phát video
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     * @throws IOException nếu có lỗi khi tải dữ liệu
     */
    public static StreamInfoTwoPhase getInfo(@Nonnull final YoutubeStreamExtractorTwoPhase extractor)
            throws ExtractionException, IOException {
        // Tải dữ liệu cần thiết (giai đoạn 1)
        extractor.fetchEssentialData(NewPipe.getDownloader());
        
        final StreamInfoTwoPhase streamInfo;
        try {
            // Trích xuất dữ liệu quan trọng
            streamInfo = extractImportantData(extractor);
            
            // Trích xuất các luồng video và âm thanh
            extractStreams(streamInfo, extractor);
            
            return streamInfo;
        } catch (final ExtractionException e) {
            final String errorMessage = extractor.getErrorMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                throw e;
            } else {
                throw new ContentNotAvailableException(errorMessage, e);
            }
        }
    }

    /**
     * Tải dữ liệu bổ sung (giai đoạn 2).
     * Phương thức này nên được gọi sau khi video đã bắt đầu phát.
     *
     * @throws IOException nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public void loadAdditionalData() throws IOException, ExtractionException {
        if (additionalDataLoaded) {
            return;
        }
        
        // Tải dữ liệu bổ sung (giai đoạn 2)
        extractor.fetchAdditionalData(NewPipe.getDownloader());
        
        // Trích xuất dữ liệu bổ sung
        extractOptionalData(this, extractor);
        
        additionalDataLoaded = true;
    }

    /**
     * Kiểm tra xem dữ liệu bổ sung đã được tải hay chưa.
     *
     * @return true nếu dữ liệu bổ sung đã được tải, false nếu chưa
     */
    public boolean isAdditionalDataLoaded() {
        return additionalDataLoaded;
    }

    @Nonnull
    private static StreamInfoTwoPhase extractImportantData(@Nonnull final YoutubeStreamExtractorTwoPhase extractor)
            throws ExtractionException {
        // Dữ liệu quan trọng, không có nó nội dung không thể hiển thị.
        // Nếu một trong những dữ liệu này không có sẵn, frontend sẽ nhận được một ngoại lệ trực tiếp.

        final String url = extractor.getUrl();
        final StreamType streamType = extractor.getStreamType();
        final String id = extractor.getId();
        final String name = extractor.getName();
        final int ageLimit = extractor.getAgeLimit();

        if (streamType == StreamType.NONE
                || url == null || url.isEmpty()
                || id == null || id.isEmpty()
                || name == null /* nhưng nó có thể trống */
                || ageLimit == -1) {
            throw new ExtractionException("Một số thông tin stream quan trọng không được cung cấp.");
        }

        return new StreamInfoTwoPhase(extractor.getServiceId(), url, extractor.getOriginalUrl(),
                streamType, id, name, ageLimit, extractor);
    }

    private static void extractStreams(final StreamInfoTwoPhase streamInfo,
                                      final YoutubeStreamExtractorTwoPhase extractor)
            throws ExtractionException {
        /* ---- Trích xuất stream diễn ra ở đây ---- */
        // Ít nhất một loại stream phải có sẵn, nếu không một ngoại lệ sẽ được ném
        // trực tiếp vào frontend.

        try {
            streamInfo.setDashMpdUrl(extractor.getDashMpdUrl());
        } catch (final Exception e) {
            streamInfo.addError(new ExtractionException("Không thể lấy DASH manifest", e));
        }

        try {
            streamInfo.setHlsUrl(extractor.getHlsUrl());
        } catch (final Exception e) {
            streamInfo.addError(new ExtractionException("Không thể lấy HLS manifest", e));
        }

        try {
            streamInfo.setAudioStreams(extractor.getEssentialAudioStreams());
        } catch (final Exception e) {
            streamInfo.addError(new ExtractionException("Không thể lấy luồng âm thanh", e));
        }

        try {
            streamInfo.setVideoStreams(extractor.getEssentialVideoStreams());
        } catch (final Exception e) {
            streamInfo.addError(new ExtractionException("Không thể lấy luồng video", e));
        }

        try {
            streamInfo.setVideoOnlyStreams(extractor.getVideoOnlyStreams());
        } catch (final Exception e) {
            streamInfo.addError(new ExtractionException("Không thể lấy luồng chỉ có video", e));
        }

        // Hoặc âm thanh hoặc video phải có sẵn, nếu không chúng ta không có stream
        // (vì videoOnly là tùy chọn, chúng không được tính).
        if ((streamInfo.videoStreams.isEmpty()) && (streamInfo.audioStreams.isEmpty())) {
            throw new StreamExtractor.StreamExtractException(
                    "Không thể lấy bất kỳ stream nào. Xem biến lỗi để biết thêm chi tiết.");
        }
    }
} 