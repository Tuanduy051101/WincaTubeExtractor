/*
 * Created by WincaTubeExtractor on 14.03.2024.
 *
 * Copyright (C) 2024 WincaTubeExtractor
 * YoutubeStreamExtractorHelper.java is part of NewPipe Extractor.
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

package org.schabi.newpipe.extractor.services.youtube;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.InfoItemsCollector;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractorTwoPhase;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Lớp tiện ích giúp ứng dụng Android sử dụng cơ chế tải 2 giai đoạn một cách dễ dàng
 */
public class YoutubeStreamExtractorHelper {

    private YoutubeStreamExtractorHelper() {
        // Ngăn khởi tạo
    }

    /**
     * Tải thông tin cần thiết để phát video ngay lập tức (giai đoạn 1)
     * @param url URL của video YouTube
     * @return Đối tượng StreamInfo với thông tin cần thiết để phát video
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     * @throws IOException nếu có lỗi khi tải dữ liệu
     */
    public static StreamInfo getEssentialInfo(final String url)
            throws ExtractionException, IOException {
        // Lấy dịch vụ YouTube
        final StreamingService service = NewPipe.getServiceByUrl(url);
        if (service == null) {
            throw new ExtractionException("Không thể xác định dịch vụ cho URL: " + url);
        }

        // Tạo link handler
        final LinkHandlerFactory linkHandlerFactory = service.getStreamLHFactory();
        if (!(linkHandlerFactory instanceof YoutubeStreamLinkHandlerFactory)) {
            throw new ExtractionException("URL không phải là URL video YouTube hợp lệ");
        }

        // Tạo extractor và tải dữ liệu cần thiết
        final YoutubeStreamExtractorTwoPhase extractor = new YoutubeStreamExtractorTwoPhase(
                service, linkHandlerFactory.fromUrl(url));
        extractor.fetchEssentialData(NewPipe.getDownloader());
        
        // Tạo đối tượng StreamInfo với thông tin cần thiết
        final StreamInfo streamInfo = new StreamInfo(
                service.getServiceId(),
                extractor.getUrl(),
                extractor.getOriginalUrl(),
                extractor.getStreamType(),
                extractor.getId(),
                extractor.getName(),
                extractor.getAgeLimit()
        );

        // Thiết lập các thông tin cần thiết
        streamInfo.setVideoStreams(extractor.getEssentialVideoStreams());
        streamInfo.setAudioStreams(extractor.getEssentialAudioStreams());
        streamInfo.setVideoOnlyStreams(extractor.getVideoOnlyStreams());
        
        // Thông tin cơ bản về người tải lên
        try {
            streamInfo.setUploaderName(extractor.getUploaderName());
        } catch (final Exception e) {
            streamInfo.addError(e);
        }
        
        try {
            streamInfo.setUploaderUrl(extractor.getUploaderUrl());
        } catch (final Exception e) {
            streamInfo.addError(e);
        }
        
        // Thông tin về thời lượng video
        try {
            streamInfo.setDuration(extractor.getLength());
        } catch (final Exception e) {
            streamInfo.addError(e);
        }
        
        // Hình thu nhỏ
        try {
            streamInfo.setThumbnails(extractor.getThumbnails());
        } catch (final Exception e) {
            streamInfo.addError(e);
        }
        
        return streamInfo;
    }
    
    /**
     * Tải thông tin bổ sung dưới nền (giai đoạn 2)
     * @param url URL của video YouTube
     * @param executorService ExecutorService để thực hiện tải dữ liệu bất đồng bộ
     * @return Future chứa đối tượng StreamInfo đầy đủ
     */
    public static Future<StreamInfo> getAdditionalInfoAsync(final String url,
                                                           final ExecutorService executorService) {
        return executorService.submit(() -> {
            try {
                // Lấy dịch vụ YouTube
                final StreamingService service = NewPipe.getServiceByUrl(url);
                if (service == null) {
                    throw new ExtractionException("Không thể xác định dịch vụ cho URL: " + url);
                }

                // Tạo link handler
                final LinkHandlerFactory linkHandlerFactory = service.getStreamLHFactory();
                if (!(linkHandlerFactory instanceof YoutubeStreamLinkHandlerFactory)) {
                    throw new ExtractionException("URL không phải là URL video YouTube hợp lệ");
                }

                // Tạo extractor và tải dữ liệu bổ sung
                final YoutubeStreamExtractorTwoPhase extractor = new YoutubeStreamExtractorTwoPhase(
                        service, linkHandlerFactory.fromUrl(url));
                extractor.fetchAdditionalData(NewPipe.getDownloader());
                
                // Tạo đối tượng StreamInfo đầy đủ
                final StreamInfo streamInfo = new StreamInfo(
                        service.getServiceId(),
                        extractor.getUrl(),
                        extractor.getOriginalUrl(),
                        extractor.getStreamType(),
                        extractor.getId(),
                        extractor.getName(),
                        extractor.getAgeLimit()
                );

                // Thiết lập các thông tin đầy đủ
                // Streams
                streamInfo.setDashMpdUrl(extractor.getDashMpdUrl());
                streamInfo.setHlsUrl(extractor.getHlsUrl());
                streamInfo.setVideoStreams(extractor.getVideoStreams());
                streamInfo.setAudioStreams(extractor.getAudioStreams());
                streamInfo.setVideoOnlyStreams(extractor.getVideoOnlyStreams());
                
                // Thông tin người tải lên
                try {
                    streamInfo.setUploaderName(extractor.getUploaderName());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setUploaderUrl(extractor.getUploaderUrl());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setUploaderAvatars(extractor.getUploaderAvatars());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setUploaderVerified(extractor.isUploaderVerified());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setUploaderSubscriberCount(extractor.getUploaderSubscriberCount());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                // Thông tin kênh phụ
                try {
                    streamInfo.setSubChannelName(extractor.getSubChannelName());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setSubChannelUrl(extractor.getSubChannelUrl());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setSubChannelAvatars(extractor.getSubChannelAvatars());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                // Thông tin video
                try {
                    streamInfo.setThumbnails(extractor.getThumbnails());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setDuration(extractor.getLength());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setDescription(extractor.getDescription());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setViewCount(extractor.getViewCount());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setTextualUploadDate(extractor.getTextualUploadDate());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setUploadDate(extractor.getUploadDate());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setLikeCount(extractor.getLikeCount());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setDislikeCount(extractor.getDislikeCount());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                // Thông tin bổ sung
                try {
                    streamInfo.setCategory(extractor.getCategory());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setLicence(extractor.getLicence());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setTags(extractor.getTags());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                try {
                    streamInfo.setMetaInfo(extractor.getMetaInfo());
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                // Video liên quan
                try {
                    InfoItemsCollector<? extends InfoItem, ?> relatedItems = extractor.getRelatedItems();
                    if (relatedItems != null) {
                        List<InfoItem> items = new ArrayList<>(relatedItems.getItems());
                        streamInfo.setRelatedItems(items);
                    }
                } catch (final Exception e) {
                    streamInfo.addError(e);
                }
                
                return streamInfo;
            } catch (final Exception e) {
                throw new RuntimeException("Lỗi khi tải thông tin bổ sung", e);
            }
        });
    }
} 