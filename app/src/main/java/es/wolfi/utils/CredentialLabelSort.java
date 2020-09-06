package es.wolfi.utils;

import java.util.Comparator;

import es.wolfi.passman.API.Credential;

/**
 * Created by wolfi on 12/11/17.
 */

public class CredentialLabelSort implements Comparator<Credential> {

    @Override
    public int compare(Credential left, Credential right) {
        return left.getLabel().compareTo(right.getLabel());
    }
}
