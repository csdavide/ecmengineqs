package it.doqui.index.ecmengineqs.business.services;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.quarkus.panache.common.Sort;
import it.doqui.index.ecmengineqs.business.dto.MimeTypeDTO;
import it.doqui.index.ecmengineqs.business.repositories.MimeTypeRepository;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MimeTypeManager {

    private final Multimap<String,MimeTypeDTO> mapByExtension;
    private final Multimap<String,MimeTypeDTO> mapByMimetype;

    @Inject
    MimeTypeRepository mimeTypeRepository;

    public MimeTypeManager() {
        mapByExtension = ArrayListMultimap.create();
        mapByMimetype = ArrayListMultimap.create();
    }

    @PostConstruct
    void init() {
        mimeTypeRepository
            .findAll(Sort.ascending("priority"))
            .stream()
            .forEach(m -> {
                MimeTypeDTO x = new MimeTypeDTO();
                x.setFileExtension(m.getFileExtension());
                x.setMimetype(m.getMimeType());
                mapByExtension.put(m.getFileExtension(),x);
                mapByMimetype.put(m.getMimeType(), x);
            });
    }

    public List<MimeTypeDTO> list(MimeTypeDTO criteria) {
        List<MimeTypeDTO> result = new ArrayList<>();
        Multimap<String,MimeTypeDTO> map = null;
        String key = null;
        if (!StringUtils.isBlank(criteria.getFileExtension())) {
            map = this.mapByExtension;
            key = criteria.getFileExtension();
            if (StringUtils.startsWith(key,".")) {
                key = key.substring(1);
            }
        } else if (!StringUtils.isBlank(criteria.getMimetype())) {
            map = this.mapByMimetype;
            key = criteria.getMimetype();
        }

        if (map != null) {
            map.get(key).forEach(result::add);
        }

        return result;
    }
}
