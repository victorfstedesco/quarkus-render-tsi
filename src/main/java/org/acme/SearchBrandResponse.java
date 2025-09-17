package org.acme;

import java.util.ArrayList;
import java.util.List;

public class SearchBrandResponse {
    public List<Brand> Brand = new ArrayList<>();
    public long TotalBrand;
    public int TotalPages;
    public boolean HasMore;
    public String NextPage;
}
