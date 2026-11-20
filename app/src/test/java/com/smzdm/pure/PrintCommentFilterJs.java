package com.smzdm.pure;

/** Exports the actual injected script for the Node DOM regression test. */
public final class PrintCommentFilterJs {
    public static void main(String[] args) {
        System.out.print(SmzdmCommentFilter.getCommentFilterJs());
    }
}
