package org.acme;

import java.util.ArrayList;
import java.util.List;

public class SearchSegmentResponse {
    public List<Segment> segment = new ArrayList<>();
    public long totalSegment;
    public int totalPages;
    public boolean hasMore;
    public String nextPage;
}