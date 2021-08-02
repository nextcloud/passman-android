package es.wolfi.utils;

import java.util.Comparator;

import es.wolfi.passman.API.Credential;

/**
 * Created by wolfi on 12/11/17.
 */

public class CredentialLabelSort implements Comparator<Credential> {

    private final int method;

    public CredentialLabelSort(int method) {
        this.method = method;
    }

    @Override
    public int compare(Credential left, Credential right) {
        if (method == 1) {
            return left.getLabel().compareTo(right.getLabel());
        }
        if (method == 2) {
            return right.getLabel().compareTo(left.getLabel());
        }
        return left.getId() - right.getId();
    }
}
