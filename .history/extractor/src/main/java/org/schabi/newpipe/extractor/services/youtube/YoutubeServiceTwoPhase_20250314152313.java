package org.schabi.newpipe.extractor.services.youtube;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractorTwoPhase;
import org.schabi.newpipe.extractor.stream.StreamExtractor;

/**
 * Một phiên bản cải tiến của YoutubeService sử dụng YoutubeStreamExtractorTwoPhase
 * để hỗ trợ tải dữ liệu theo 2 giai đoạn.
 */
public class YoutubeServiceTwoPhase extends YoutubeService {

    public YoutubeServiceTwoPhase(final int id) {
        super(id);
    }

    /**
     * Ghi đè phương thức getStreamExtractor để sử dụng YoutubeStreamExtractorTwoPhase
     * thay vì YoutubeStreamExtractor.
     */
    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new YoutubeStreamExtractorTwoPhase(this, linkHandler);
    }
} 