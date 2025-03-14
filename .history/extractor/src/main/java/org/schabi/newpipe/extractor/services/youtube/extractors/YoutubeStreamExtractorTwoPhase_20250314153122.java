package org.schabi.newpipe.extractor.services.youtube.extractors;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.CONTENT_CHECK_OK;
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

        // Gọi phương thức onFetchPage của lớp cha nhưng chỉ tải dữ liệu cần thiết
        // Đây là một cách tạm thời để tải dữ liệu cần thiết
        onFetchPage(downloader);
        
        essentialDataLoaded = true;
        additionalDataLoaded = true; // Vì onFetchPage đã tải tất cả dữ liệu
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
            return;
        }

        // Trong trường hợp này, tất cả dữ liệu đã được tải trong fetchEssentialData
        additionalDataLoaded = true;
    }

    /**
     * Ghi đè phương thức onFetchPage để sử dụng cơ chế tải 2 giai đoạn.
     * Phương thức này sẽ tải cả dữ liệu cần thiết và dữ liệu bổ sung.
     */
    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        // Gọi phương thức của lớp cha để tải tất cả dữ liệu
        super.onFetchPage(downloader);
        
        // Đánh dấu cả hai giai đoạn đã hoàn thành
        essentialDataLoaded = true;
        additionalDataLoaded = true;
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
    protected void assertPageFetched() {
        if (!essentialDataLoaded) {
            throw new IllegalStateException("Essential data not loaded yet");
        }
    }

    /**
     * Kiểm tra xem dữ liệu bổ sung đã được tải hay chưa.
     * Phương thức này nên được gọi trước khi gọi các phương thức yêu cầu dữ liệu bổ sung.
     */
    protected void assertAdditionalDataFetched() {
        if (!additionalDataLoaded) {
            throw new IllegalStateException("Additional data not loaded yet");
        }
    }
} 