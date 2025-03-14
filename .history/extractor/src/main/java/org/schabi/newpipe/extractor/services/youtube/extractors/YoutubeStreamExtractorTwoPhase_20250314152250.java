package org.schabi.newpipe.extractor.services.youtube.extractors;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.NEXT;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.RACY_CHECK_OK;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.VIDEO_ID;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getJsonPostResponse;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;

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
 * Một phiên bản cải tiến của YoutubeStreamExtractor hỗ trợ tải dữ liệu theo 2 giai đoạn.
 * <p>
 * Giai đoạn 1: Tải các thông tin cần thiết để phát video ngay lập tức (URL phát, định dạng video/audio)
 * Giai đoạn 2: Tải các thông tin bổ sung dưới nền (mô tả, bình luận, video liên quan, v.v.)
 */
public class YoutubeStreamExtractorTwoPhase extends YoutubeStreamExtractor {

    private boolean essentialDataLoaded = false;
    private boolean additionalDataLoaded = false;

    public YoutubeStreamExtractorTwoPhase(final StreamingService service, final LinkHandler linkHandler) {
        super(service, linkHandler);
    }

    /**
     * Tải các thông tin cần thiết để phát video ngay lập tức.
     * Phương thức này nên được gọi trước khi gọi các phương thức getEssentialXXX().
     *
     * @param downloader Downloader để tải dữ liệu
     * @throws IOException        nếu có lỗi khi tải dữ liệu
     * @throws ExtractionException nếu có lỗi khi trích xuất dữ liệu
     */
    public void fetchEssentialData(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        if (essentialDataLoaded) {
            return;
        }

        final String videoId = getId();
        final Localization localization = getExtractorLocalization();
        final ContentCountry contentCountry = getExtractorContentCountry();

        final PoTokenProvider poTokenproviderInstance = poTokenProvider;
        final boolean noPoTokenProviderSet = poTokenproviderInstance == null;

        // Chỉ tải dữ liệu từ client HTML5 để có được URL phát nhanh nhất
        fetchHtml5Client(localization, contentCountry, videoId, poTokenproviderInstance,
                noPoTokenProviderSet);

        setStreamType();

        essentialDataLoaded = true;
    }

    /**
     * Tải các thông tin bổ sung dưới nền.
     * Phương thức này nên được gọi sau khi gọi fetchEssentialData() và video đã bắt đầu phát.
     *
     * @param downloader Downloader để tải dữ liệu
     * @throws IOException        nếu có lỗi khi tải dữ liệu
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

        final String videoId = getId();
        final Localization localization = getExtractorLocalization();
        final ContentCountry contentCountry = getExtractorContentCountry();

        final PoTokenProvider poTokenproviderInstance = poTokenProvider;
        final boolean noPoTokenProviderSet = poTokenproviderInstance == null;

        // Tải dữ liệu từ client Android để có thêm thông tin
        final PoTokenResult androidPoTokenResult = noPoTokenProviderSet ? null
                : poTokenproviderInstance.getAndroidClientPoToken(videoId);
        fetchAndroidClient(localization, contentCountry, videoId, androidPoTokenResult);

        // Tải dữ liệu từ client iOS nếu cần
        if (fetchIosClient) {
            final PoTokenResult iosPoTokenResult = noPoTokenProviderSet ? null
                    : poTokenproviderInstance.getIosClientPoToken(videoId);
            fetchIosClient(localization, contentCountry, videoId, iosPoTokenResult);
        }

        // Tải dữ liệu "next" (video liên quan, v.v.)
        final byte[] nextBody = JsonWriter.string(
                prepareDesktopJsonBuilder(localization, contentCountry)
                        .value(VIDEO_ID, videoId)
                        .value(CONTENT_CHECK_OK, true)
                        .value(RACY_CHECK_OK, true)
                        .done())
                .getBytes(StandardCharsets.UTF_8);
        nextResponse = getJsonPostResponse(NEXT, nextBody, localization);

        additionalDataLoaded = true;
    }

    /**
     * Ghi đè phương thức onFetchPage để sử dụng cơ chế tải 2 giai đoạn.
     * Phương thức này sẽ tải cả dữ liệu cần thiết và dữ liệu bổ sung.
     */
    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        fetchEssentialData(downloader);
        fetchAdditionalData(downloader);
    }

    /**
     * Lấy danh sách các luồng video cần thiết để phát video ngay lập tức.
     * Phương thức này chỉ yêu cầu dữ liệu từ giai đoạn 1.
     */
    public List<VideoStream> getEssentialVideoStreams() throws ExtractionException {
        if (!essentialDataLoaded) {
            throw new ExtractionException("Essential data not loaded yet");
        }

        return getVideoStreams();
    }

    /**
     * Lấy danh sách các luồng âm thanh cần thiết để phát video ngay lập tức.
     * Phương thức này chỉ yêu cầu dữ liệu từ giai đoạn 1.
     */
    public List<AudioStream> getEssentialAudioStreams() throws ExtractionException {
        if (!essentialDataLoaded) {
            throw new ExtractionException("Essential data not loaded yet");
        }

        return getAudioStreams();
    }

    /**
     * Kiểm tra xem dữ liệu cần thiết đã được tải hay chưa.
     */
    public boolean isEssentialDataLoaded() {
        return essentialDataLoaded;
    }

    /**
     * Kiểm tra xem dữ liệu bổ sung đã được tải hay chưa.
     */
    public boolean isAdditionalDataLoaded() {
        return additionalDataLoaded;
    }

    /**
     * Ghi đè phương thức assertPageFetched để kiểm tra xem dữ liệu cần thiết đã được tải hay chưa.
     */
    @Override
    protected void assertPageFetched() throws ExtractionException {
        if (!essentialDataLoaded) {
            throw new ExtractionException("Essential data not loaded yet");
        }
    }

    /**
     * Kiểm tra xem dữ liệu bổ sung đã được tải hay chưa.
     * Phương thức này nên được gọi trước khi gọi các phương thức yêu cầu dữ liệu bổ sung.
     */
    protected void assertAdditionalDataFetched() throws ExtractionException {
        if (!additionalDataLoaded) {
            throw new ExtractionException("Additional data not loaded yet");
        }
    }
} 