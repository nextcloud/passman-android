package es.wolfi.utils;

import java.util.Comparator;

import es.wolfi.passman.API.Credential;

/**
 * Created by wolfi on 12/11/17.
 */

public class CredentialLabelSort implements Comparator<Credential> {

    /**
     * credential sort methods:
     * description                      code
     * <p>
     * default server sort              0
     * alphabetically ascending         1
     * alphabetically descending        2
     */
    public enum sortMethod {
        standard, alphabeticallyAscending, alphabeticallyDescending
    }

    private final int method;

    public CredentialLabelSort(int method) {
        this.method = method;
    }

    @Override
    public int compare(Credential left, Credential right) {
        if (method == sortMethod.alphabeticallyAscending.ordinal()) {
            return left.getLabel().compareTo(right.getLabel());
        }
        if (method == sortMethod.alphabeticallyDescending.ordinal()) {
            return right.getLabel().compareTo(left.getLabel());
        }
        return left.getId() - right.getId();
    }
}
