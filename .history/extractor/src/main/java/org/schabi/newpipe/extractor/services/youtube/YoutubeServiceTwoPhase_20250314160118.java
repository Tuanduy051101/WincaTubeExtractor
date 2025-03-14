/*
 * Created by WincaTubeExtractor on 14.03.2024.
 *
 * Copyright (C) 2024 WincaTubeExtractor
 * YoutubeServiceTwoPhase.java is part of NewPipe Extractor.
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

import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractorTwoPhase;
import org.schabi.newpipe.extractor.stream.StreamExtractor;

/**
 * Lớp dịch vụ YouTube với cơ chế tải 2 giai đoạn.
 * Lớp này kế thừa từ YoutubeService và ghi đè phương thức getStreamExtractor
 * để trả về một đối tượng YoutubeStreamExtractorTwoPhase thay vì YoutubeStreamExtractor.
 */
public class YoutubeServiceTwoPhase extends YoutubeService {

    /**
     * Khởi tạo một dịch vụ YouTube với cơ chế tải 2 giai đoạn.
     *
     * @param id ID của dịch vụ
     */
    public YoutubeServiceTwoPhase(final int id) {
        super(id);
    }

    /**
     * Trả về một đối tượng YoutubeStreamExtractorTwoPhase thay vì YoutubeStreamExtractor.
     *
     * @param linkHandler Trình xử lý liên kết
     * @return Đối tượng YoutubeStreamExtractorTwoPhase
     */
    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new YoutubeStreamExtractorTwoPhase(this, linkHandler);
    }
} 