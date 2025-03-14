package org.schabi.newpipe.extractor.services.youtube;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractorTwoPhase;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Lớp tiện ích để giúp ứng dụng Android sử dụng cơ chế tải 2 giai đoạn một cách dễ dàng.
 */
public class YoutubeStreamExtractorHelper {

    private YoutubeStreamExtractorHelper() {
        // Không cho phép tạo đối tượng
    }

    /**
     * Tải thông tin cần thiết để phát video ngay lập tức.
     *
     * @param url URL của video YouTube
     * @return Đối tượng StreamInfo chứa thông tin cần thiết để phát video
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     * @throws IOException        nếu có lỗi khi tải dữ liệu
     */
    public static StreamInfo getEssentialInfo(final String url)
            throws ExtractionException, IOException {
        final LinkHandlerFactory factory = ServiceList.YouTubeTwoPhase.getStreamLHFactory();
        final LinkHandler linkHandler = factory.fromUrl(url);
        final YoutubeStreamExtractorTwoPhase extractor =
                (YoutubeStreamExtractorTwoPhase) ServiceList.YouTubeTwoPhase.getStreamExtractor(linkHandler);

        // Tải dữ liệu cần thiết
        extractor.fetchEssentialData(NewPipe.getDownloader());

        // Tạo đối tượng StreamInfo với thông tin cần thiết
        final StreamInfo.Builder builder = new StreamInfo.Builder()
                .setServiceId(ServiceList.YouTubeTwoPhase.getServiceId())
                .setUrl(url)
                .setOriginalUrl(url)
                .setId(extractor.getId())
                .setName(extractor.getName())
                .setUploaderName(extractor.getUploaderName())
                .setUploaderUrl(extractor.getUploaderUrl())
                .setThumbnailUrl(extractor.getThumbnailUrl())
                .setDuration(extractor.getLength())
                .setAudioStreams(extractor.getEssentialAudioStreams())
                .setVideoStreams(extractor.getEssentialVideoStreams())
                .setVideoOnlyStreams(extractor.getVideoOnlyStreams());

        return builder.build();
    }

    /**
     * Tải thông tin bổ sung dưới nền.
     *
     * @param url            URL của video YouTube
     * @param executorService ExecutorService để chạy tác vụ dưới nền
     * @return Future chứa đối tượng StreamInfo đầy đủ
     */
    public static Future<StreamInfo> getAdditionalInfoAsync(final String url,
                                                           final ExecutorService executorService) {
        return executorService.submit(new Callable<StreamInfo>() {
            @Override
            public StreamInfo call() throws Exception {
                final LinkHandlerFactory factory = ServiceList.YouTubeTwoPhase.getStreamLHFactory();
                final LinkHandler linkHandler = factory.fromUrl(url);
                final YoutubeStreamExtractorTwoPhase extractor =
                        (YoutubeStreamExtractorTwoPhase) ServiceList.YouTubeTwoPhase.getStreamExtractor(linkHandler);

                // Tải dữ liệu bổ sung
                extractor.fetchAdditionalData(NewPipe.getDownloader());

                // Tạo đối tượng StreamInfo đầy đủ
                final StreamInfo.Builder builder = new StreamInfo.Builder()
                        .setServiceId(ServiceList.YouTubeTwoPhase.getServiceId())
                        .setUrl(url)
                        .setOriginalUrl(url)
                        .setId(extractor.getId())
                        .setName(extractor.getName())
                        .setUploaderName(extractor.getUploaderName())
                        .setUploaderUrl(extractor.getUploaderUrl())
                        .setThumbnailUrl(extractor.getThumbnailUrl())
                        .setDuration(extractor.getLength())
                        .setAudioStreams(extractor.getAudioStreams())
                        .setVideoStreams(extractor.getVideoStreams())
                        .setVideoOnlyStreams(extractor.getVideoOnlyStreams());

                // Thêm các thông tin bổ sung nếu có
                try {
                    builder.setDescription(extractor.getDescription());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được mô tả
                }

                try {
                    builder.setViewCount(extractor.getViewCount());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được lượt xem
                }

                try {
                    builder.setUploadDate(extractor.getUploadDate());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được ngày tải lên
                }

                try {
                    builder.setUploaderAvatarUrl(extractor.getUploaderAvatarUrl());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được avatar người tải lên
                }

                try {
                    builder.setSubChannelUrl(extractor.getSubChannelUrl());
                    builder.setSubChannelName(extractor.getSubChannelName());
                    builder.setSubChannelAvatarUrl(extractor.getSubChannelAvatarUrl());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được thông tin kênh phụ
                }

                try {
                    builder.setLikeCount(extractor.getLikeCount());
                    builder.setDislikeCount(extractor.getDislikeCount());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được lượt thích/không thích
                }

                try {
                    builder.setSubscriberCount(extractor.getSubscriberCount());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được số người đăng ký
                }

                try {
                    builder.setCategory(extractor.getCategory());
                    builder.setTags(extractor.getTags());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được danh mục và thẻ
                }

                try {
                    builder.setFeedUrl(extractor.getFeedUrl());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được URL feed
                }

                try {
                    builder.setRelatedItems(extractor.getRelatedItems());
                } catch (Exception e) {
                    // Bỏ qua nếu không lấy được video liên quan
                }

                return builder.build();
            }
        });
    }

    /**
     * Lấy danh sách các luồng video cần thiết để phát video ngay lập tức.
     *
     * @param url URL của video YouTube
     * @return Danh sách các luồng video
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     * @throws IOException        nếu có lỗi khi tải dữ liệu
     */
    public static List<VideoStream> getEssentialVideoStreams(final String url)
            throws ExtractionException, IOException {
        final LinkHandlerFactory factory = ServiceList.YouTubeTwoPhase.getStreamLHFactory();
        final LinkHandler linkHandler = factory.fromUrl(url);
        final YoutubeStreamExtractorTwoPhase extractor =
                (YoutubeStreamExtractorTwoPhase) ServiceList.YouTubeTwoPhase.getStreamExtractor(linkHandler);

        // Tải dữ liệu cần thiết
        extractor.fetchEssentialData(NewPipe.getDownloader());

        // Lấy danh sách các luồng video
        return extractor.getEssentialVideoStreams();
    }

    /**
     * Lấy danh sách các luồng âm thanh cần thiết để phát video ngay lập tức.
     *
     * @param url URL của video YouTube
     * @return Danh sách các luồng âm thanh
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     * @throws IOException        nếu có lỗi khi tải dữ liệu
     */
    public static List<AudioStream> getEssentialAudioStreams(final String url)
            throws ExtractionException, IOException {
        final LinkHandlerFactory factory = ServiceList.YouTubeTwoPhase.getStreamLHFactory();
        final LinkHandler linkHandler = factory.fromUrl(url);
        final YoutubeStreamExtractorTwoPhase extractor =
                (YoutubeStreamExtractorTwoPhase) ServiceList.YouTubeTwoPhase.getStreamExtractor(linkHandler);

        // Tải dữ liệu cần thiết
        extractor.fetchEssentialData(NewPipe.getDownloader());

        // Lấy danh sách các luồng âm thanh
        return extractor.getEssentialAudioStreams();
    }
} 