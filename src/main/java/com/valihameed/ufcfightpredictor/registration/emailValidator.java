package com.valihameed.ufcfightpredictor.registration;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;
import java.util.function.Predicate;
@Service
public class emailValidator implements Predicate<String> {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

    @Override
    public boolean test(String s) {
        return s != null && EMAIL_PATTERN.matcher(s).matches();
    }
}
