package it.doqui.index.ecmengineqs.business.dto;

import it.doqui.index.ecmengineqs.foundation.Localizable;
import it.doqui.index.ecmengineqs.utils.I18NUtils;

import java.util.HashMap;
import java.util.Locale;

public class MLTextProperty extends HashMap<Locale,Object> implements Localizable {

    @Override
    public Object getLocalizedValue(Locale locale) {
        Locale closestLocale = I18NUtils.getNearestLocale(locale, this.keySet());
        return this.get(closestLocale);
    }
}
