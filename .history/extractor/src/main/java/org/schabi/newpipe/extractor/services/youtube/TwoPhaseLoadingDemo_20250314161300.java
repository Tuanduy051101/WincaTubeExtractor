/*
 * Created by WincaTubeExtractor on 14.03.2024.
 *
 * Copyright (C) 2024 WincaTubeExtractor
 * TwoPhaseLoadingDemo.java is part of NewPipe Extractor.
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

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Lớp demo minh họa cách sử dụng cơ chế tải 2 giai đoạn
 */
public class TwoPhaseLoadingDemo {

    /**
     * Phương thức main để chạy demo
     * @param args Tham số dòng lệnh
     */
    public static void main(String[] args) {
        // URL video YouTube để thử nghiệm
        final String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
        
        // Tạo ExecutorService để thực hiện tải dữ liệu bất đồng bộ
        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        
        try {
            System.out.println("=== Demo cơ chế tải 2 giai đoạn ===");
            
            // Giai đoạn 1: Tải thông tin cần thiết để phát video ngay lập tức
            System.out.println("\n=== Giai đoạn 1: Tải thông tin cần thiết ===");
            final long startTime1 = System.currentTimeMillis();
            
            final StreamInfo essentialInfo = YoutubeStreamExtractorHelper.getEssentialInfo(videoUrl);
            
            final long endTime1 = System.currentTimeMillis();
            System.out.println("Thời gian tải giai đoạn 1: " + (endTime1 - startTime1) + "ms");
            
            // In thông tin cần thiết
            System.out.println("Tiêu đề: " + essentialInfo.getName());
            System.out.println("Người tải lên: " + essentialInfo.getUploaderName());
            System.out.println("Thời lượng: " + formatDuration(essentialInfo.getDuration()));
            System.out.println("Số luồng video: " + essentialInfo.getVideoStreams().size());
            System.out.println("Số luồng âm thanh: " + essentialInfo.getAudioStreams().size());
            
            // Giả lập việc bắt đầu phát video
            System.out.println("\n=== Bắt đầu phát video ===");
            System.out.println("Video đang phát...");
            
            // Giai đoạn 2: Tải thông tin bổ sung dưới nền
            System.out.println("\n=== Giai đoạn 2: Tải thông tin bổ sung dưới nền ===");
            final long startTime2 = System.currentTimeMillis();
            
            final Future<StreamInfo> additionalInfoFuture = 
                    YoutubeStreamExtractorHelper.getAdditionalInfoAsync(videoUrl, executorService);
            
            // Giả lập việc phát video trong khi tải thông tin bổ sung
            System.out.println("Video tiếp tục phát trong khi tải thông tin bổ sung...");
            
            // Đợi thông tin bổ sung được tải xong
            final StreamInfo fullInfo = additionalInfoFuture.get();
            
            final long endTime2 = System.currentTimeMillis();
            System.out.println("Thời gian tải giai đoạn 2: " + (endTime2 - startTime2) + "ms");
            
            // In thông tin bổ sung
            System.out.println("\n=== Thông tin bổ sung ===");
            System.out.println("Mô tả: " + (fullInfo.getDescription() != null 
                    ? fullInfo.getDescription().getContent().substring(0, 100) + "..." 
                    : "Không có"));
            System.out.println("Lượt xem: " + formatViewCount(fullInfo.getViewCount()));
            System.out.println("Lượt thích: " + formatLikeCount(fullInfo.getLikeCount()));
            System.out.println("Ngày tải lên: " + fullInfo.getTextualUploadDate());
            System.out.println("Danh mục: " + fullInfo.getCategory());
            System.out.println("Số video liên quan: " + 
                    (fullInfo.getRelatedItems() != null ? fullInfo.getRelatedItems().size() : 0));
            
        } catch (ExtractionException | IOException | InterruptedException | ExecutionException e) {
            System.err.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Đóng ExecutorService
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Định dạng thời lượng video
     * @param durationSeconds Thời lượng tính bằng giây
     * @return Chuỗi định dạng thời lượng
     */
    private static String formatDuration(long durationSeconds) {
        if (durationSeconds <= 0) {
            return "Không xác định";
        }
        
        final long hours = durationSeconds / 3600;
        final long minutes = (durationSeconds % 3600) / 60;
        final long seconds = durationSeconds % 60;
        
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    /**
     * Định dạng số lượt xem
     * @param viewCount Số lượt xem
     * @return Chuỗi định dạng số lượt xem
     */
    private static String formatViewCount(long viewCount) {
        if (viewCount <= 0) {
            return "Không xác định";
        }
        
        if (viewCount < 1000) {
            return String.valueOf(viewCount);
        } else if (viewCount < 1000000) {
            return String.format("%.1fK", viewCount / 1000.0);
        } else if (viewCount < 1000000000) {
            return String.format("%.1fM", viewCount / 1000000.0);
        } else {
            return String.format("%.1fB", viewCount / 1000000000.0);
        }
    }
    
    /**
     * Định dạng số lượt thích
     * @param likeCount Số lượt thích
     * @return Chuỗi định dạng số lượt thích
     */
    private static String formatLikeCount(long likeCount) {
        if (likeCount <= 0) {
            return "Không xác định";
        }
        
        if (likeCount < 1000) {
            return String.valueOf(likeCount);
        } else if (likeCount < 1000000) {
            return String.format("%.1fK", likeCount / 1000.0);
        } else if (likeCount < 1000000000) {
            return String.format("%.1fM", likeCount / 1000000.0);
        } else {
            return String.format("%.1fB", likeCount / 1000000000.0);
        }
    }
} 