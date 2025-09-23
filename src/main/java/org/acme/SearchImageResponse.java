package org.acme;

import java.util.ArrayList;
import java.util.List;

public class SearchImageResponse {
    public List<Image> image = new ArrayList<>();
    public long totalImage;
    public int totalPages;
    public boolean hasMore;
    public String nextPage;
}
