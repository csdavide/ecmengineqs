package it.doqui.index.ecmengineqs.utils;

import java.util.*;

public class I18NUtils {

    public static Locale getNearestLocale(Locale templateLocale, Set<Locale> options) {
        if (options.isEmpty()) {
            return null;
        } else {
            Locale lastMatchingOption;
            if (templateLocale == null) {
                Iterator i$ = options.iterator();
                if (i$.hasNext()) {
                    lastMatchingOption = (Locale)i$.next();
                    return lastMatchingOption;
                }
            } else if (options.contains(templateLocale)) {
                return templateLocale;
            }

            Set<Locale> remaining = new HashSet(options);
            lastMatchingOption = null;
            String templateLanguage = templateLocale.getLanguage();
            if (templateLanguage != null && !templateLanguage.equals("")) {
                Iterator iterator = remaining.iterator();

                label81:
                while(true) {
                    while(true) {
                        if (!iterator.hasNext()) {
                            break label81;
                        }

                        Locale option = (Locale)iterator.next();
                        if (option != null && !templateLanguage.equals(option.getLanguage())) {
                            iterator.remove();
                        } else {
                            lastMatchingOption = option;
                        }
                    }
                }
            }

            if (remaining.isEmpty()) {
                return null;
            } else if (remaining.size() == 1 && lastMatchingOption != null) {
                return lastMatchingOption;
            } else {
                lastMatchingOption = null;
                String templateCountry = templateLocale.getCountry();
                Locale locale;
                Iterator i$;
                if (templateCountry != null && !templateCountry.equals("")) {
                    i$ = remaining.iterator();

                    label64:
                    while(true) {
                        do {
                            if (!i$.hasNext()) {
                                break label64;
                            }

                            locale = (Locale)i$.next();
                        } while(locale != null && !templateCountry.equals(locale.getCountry()));

                        lastMatchingOption = locale;
                    }
                }

                if (remaining.size() == 1 && lastMatchingOption != null) {
                    return lastMatchingOption;
                } else if (lastMatchingOption != null) {
                    return lastMatchingOption;
                } else {
                    i$ = remaining.iterator();
                    if (i$.hasNext()) {
                        locale = (Locale)i$.next();
                        return locale;
                    } else {
                        throw new RuntimeException("Logic should not allow code to get here.");
                    }
                }
            }
        }
    }

    public static Locale parseLocale(String localeStr) {
        if (localeStr == null) {
            return null;
        } else {
            Locale locale = Locale.getDefault();
            StringTokenizer t = new StringTokenizer(localeStr, "_");
            int tokens = t.countTokens();
            if (tokens == 1) {
                locale = new Locale(t.nextToken());
            } else if (tokens == 2) {
                locale = new Locale(t.nextToken(), t.nextToken());
            } else if (tokens == 3) {
                locale = new Locale(t.nextToken(), t.nextToken(), t.nextToken());
            }

            return locale;
        }
    }

}
