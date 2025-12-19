package com.thanh.auction_server.constants;

import lombok.Getter;

@Getter
public enum FeedbackRating {
    POSITIVE(1),
    NEUTRAL(0),
    NEGATIVE(-1);

    private final int value;

    FeedbackRating(int value) {
        this.value = value;
    }
}
