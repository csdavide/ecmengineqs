package it.doqui.index.ecmengineqs.business.search;

import it.doqui.index.ecmengine.util.ISO8601DateFormat;
import it.doqui.index.ecmengineqs.foundation.PrefixedQName;
import it.doqui.index.ecmengineqs.business.services.ModelService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

@ApplicationScoped
@Slf4j
public class SolrQueryParser {
    public static final String QUOTE_DATE_REGEX = "^\\\"*(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[0-1]|0[1-9]|[1-2][0-9])?T(2[0-3]|[0-1][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)??(Z|[+-](?:2[0-3]|[0-1][0-9]):[0-5][0-9])?\\\"*$";
    public static final String DATE_REGEX = "^(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[0-1]|0[1-9]|[1-2][0-9])?(T(2[0-3]|[0-1][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)??(Z|[+-](?:2[0-3]|[0-1][0-9]):[0-5][0-9])?)?$";

    @Inject
    ModelService modelService;

    public String parse(SolrQuery query) {
        String output = "";
        findElements(query);
        convertElements(query);
        output = generateResult(query);
        // Rimozione tonda finale in piu' (per ACTA)
        int roundOpen = StringUtils.countMatches(output, "(");
        int roundClosed = StringUtils.countMatches(output, ")");
        int escapeRoundOpen = StringUtils.countMatches(output, "\\(");
        int escapeRoundClosed = StringUtils.countMatches(output, "\\)");
        roundOpen = roundOpen - escapeRoundOpen;
        roundClosed = roundClosed - escapeRoundClosed;
        if (roundOpen == roundClosed - 1) {
            if (output.replace(" ", "").endsWith("))")) {
                output = output.substring(0, output.lastIndexOf(")"));
            }
        }

        // Rimozione doppi not
        output = output.replaceAll("NOT\\s+NOT", "");
        if (output.indexOf("##log##") != -1) {
            output = output.replaceAll("##log##", "").trim();
            log.info("######### query = " + output);
        }
        return output;
    }

    private void findElements(SolrQuery query) {
        char[] originalQuery = query.getOriginalQuery();
        boolean precEscape = false;
        boolean quotaOpen = false;
        boolean squareOpen = false;
        boolean inElem = false;
        int startElem = 0;
        int endElem = 0;
        int separator = 0;
        boolean wildCard = false;
        for (int i = 0; i < originalQuery.length; i++) {
            char character = originalQuery[i];
            if (((character == ' ' && !quotaOpen && !squareOpen) || (character == ']' && !quotaOpen)
                || (character == '\"' && quotaOpen) || i == originalQuery.length - 1) && !precEscape && inElem) {
                endElem = i;
                query.addElem(startElem, character == ' ' ? (endElem - 1) : endElem, separator, wildCard);
                wildCard = false;
                inElem = false;
            }
            if (character == '\"' && !precEscape) {
                quotaOpen = !quotaOpen;
            } else if (character == '[' && !precEscape && !squareOpen) {
                squareOpen = true;
            } else if (character == ']' && !precEscape && squareOpen) {
                squareOpen = false;
            } else if ((character == '*' || character == '?') && !precEscape && inElem && !squareOpen) {
                wildCard = true;
            } else if ((character == ':' || character == '>' || character == '<') && !precEscape && !quotaOpen
                && !squareOpen) {
                separator = i;
                int j = i;
                for (; j >= 0; j--) {
                    if (originalQuery[j] == ' ' || originalQuery[j] == '(') {
                        break;
                    }
                }
                startElem = j + 1;
                inElem = true;
            }
            if (character == '\\' && !precEscape) {
                precEscape = true;
            } else {
                precEscape = false;
            }
        }
    }

    private void convertElements(SolrQuery query) {
        char[] originalQuery = query.getOriginalQuery();
        for (SolrQueryElement element : query.getElements()) {
            String elem = new String(originalQuery).substring(element.getStart(), element.getEnd() + 1);
            if (elem.startsWith("@sys\\:node-uuid:") || elem.startsWith("+@sys\\:node-uuid:")
                || elem.startsWith("-@sys\\:node-uuid:")) {
                element.setConversion(convertNodeUUID(elem, element.getSeparator() - element.getStart()));
            } else if (elem.startsWith("@sys\\:node-dbid:") || elem.startsWith("+@sys\\:node-dbid:")
                || elem.startsWith("-@sys\\:node-dbid:")) {
                element.setConversion(convertNodeDBID(elem, element.getSeparator() - element.getStart()));
            } else if ((elem.startsWith("@") || elem.startsWith("+@") || elem.startsWith("-@"))
                && (!elem.startsWith("@\\{") && !elem.startsWith("+@\\{") && !elem.startsWith("-@\\{"))) {
                if (element.isWildCard()) {
                    element.setConversion(convertFieldName(elem, element.getSeparator() - element.getStart()) + ":"
                        + convertWildcardFieldValue(
                        elem.substring(element.getSeparator() - element.getStart() + 1)));
                } else {
                    element.setConversion(convertFieldName(elem, element.getSeparator() - element.getStart())
                        + checkDateFieldValue(elem.substring(element.getSeparator() - element.getStart())));
                }
            } else if (elem.startsWith("ASPECT:")) {
                element.setConversion(elem.substring(0, element.getSeparator() - element.getStart() + 1)
                    + convertFieldValue(elem, element.getSeparator() - element.getStart()));
            } else if (elem.startsWith("TYPE:") || elem.startsWith("EXACTTYPE:")) {
                element.setConversion(elem.substring(0, element.getSeparator() - element.getStart() + 1)
                    + convertFieldValue(elem, element.getSeparator() - element.getStart()));
            } else if (elem.startsWith("PATH:")) {
                element.setConversion(convertPath(elem, element.getSeparator() - element.getStart(), false));
            } else if (elem.startsWith("PARENTPATH:")) {
                element.setConversion(convertPath(elem, element.getSeparator() - element.getStart(), true));
            } else if (elem.startsWith("ISNULL:")) {
                element.setConversion(convertIsnull(elem, element.getSeparator() - element.getStart()));
            } else if (elem.startsWith("ISNOTNULL:")) {
                element.setConversion(convertIsNotnull(elem, element.getSeparator() - element.getStart()));
            } else if (elem.startsWith("PARENT:") || elem.startsWith("PRIMARYPARENT:")) {
                element.setConversion(convertParent(elem, element.getSeparator() - element.getStart()));
            } else {
                element.setConversion(elem);
            }
        }
    }

    private String convertNodeUUID(String elem, int separatorOffset) {
        String toReturn = "";
        String pre = "ID";
        String post = elem.substring(separatorOffset + 1);
        if (post.startsWith("\"")) {
            post = post.substring(1, post.length() - 1);
        }
        if (post.endsWith("/")) {
            post = post.substring(0, post.length() - 1);
        }
        post = "workspace://SpacesStore" + "/" + post;
        toReturn = pre + ":\"" + post + "\"";
        return toReturn;
    }

    private String convertNodeDBID(String elem, int separatorOffset) {
        String toReturn = "";
        String pre = "DBID";
        String post = elem.substring(separatorOffset + 1);
        if (post.startsWith("\"")) {
            post = post.substring(1, post.length() - 1);
        }
        if (post.endsWith("/")) {
            post = post.substring(0, post.length() - 1);
        }
        toReturn = pre + ":\"" + post + "\"";
        return toReturn;
    }

    private String convertParent(String elem, int separatorOffset) {
        String toReturn = "";
        String pre = elem.substring(0, separatorOffset);
        String post = elem.substring(separatorOffset + 1);
        if (post.startsWith("\"")) {
            post = post.substring(1, post.length() - 1);
        }
        if (post.endsWith("/")) {
            post = post.substring(0, post.length() - 1);
        }
        post = post.substring(post.lastIndexOf("/"));
        post = "workspace://SpacesStore" + post;
        toReturn = pre + ":\"" + post + "\"";
        return toReturn;
    }

    private String convertIsNotnull(String elem, int separatorOffset) {
        String toReturn = "";
        String post = elem.substring(separatorOffset + 1);
        if (post.startsWith("\"")) {
            post = post.substring(1, post.length() - 1);
        }
        if (post.endsWith("/")) {
            post = post.substring(0, post.length() - 1);
        }
        if (post.contains("\\")) {
            post = post.replace("\\", "");
        }
        if (post.contains(":")) {
            post = resolveToQName(post).toString();
            post = "@" + post.replace(":", "\\:").replace("{", "\\{").replace("}", "\\}");
        }
        toReturn = post + ":*";
        return toReturn;
    }

    private String convertIsnull(String elem, int separatorOffset) {
        String toReturn = "";
        String post = elem.substring(separatorOffset + 1);
        if (post.startsWith("\"")) {
            post = post.substring(1, post.length() - 1);
        }
        if (post.endsWith("/")) {
            post = post.substring(0, post.length() - 1);
        }
        if (post.contains("\\")) {
            post = post.replace("\\", "");
        }
        if (post.contains(":")) {
            post = resolveToQName(post).toString();
            post = "@" + post.replace(":", "\\:").replace("{", "\\{").replace("}", "\\}");
        }
        toReturn = "NOT " + post + ":[* TO *]";
        return toReturn;
    }

    private String checkDateFieldValue(String value) {
        String toReturn = value.substring(1);
        if (toReturn.startsWith("[")) {
            String noSquare = toReturn.substring(1, toReturn.length() - 1).replace(" to ", " TO ");
            String[] elems = noSquare.split(" TO ");
            if (elems != null && elems.length == 2) {
                if (elems[0].matches(DATE_REGEX)) {
                    elems[0] = formatDate(ISO8601DateFormat.parse(elems[0]));
                }
                if (elems[1].matches(DATE_REGEX)) {
                    elems[1] = formatDate(ISO8601DateFormat.parse(elems[1]));
                }
                toReturn = "[" + elems[0] + " TO " + elems[1] + "]";
            }
        } else if (toReturn.startsWith("\"")) {
            if (toReturn.matches(QUOTE_DATE_REGEX)) {
                toReturn = "\"" + formatDate(ISO8601DateFormat.parse(toReturn.substring(1, toReturn.length() - 1)))
                    + "\"";
            }
        } else {
            // TODO: Correggere
            if (toReturn.replace("\\", "").matches(DATE_REGEX)) {
                toReturn = formatDate(ISO8601DateFormat.parse(toReturn));
            }
        }
        return ":" + toReturn;
    }

    private String convertPath(String elem, int separatorOffset, boolean isParentPath) {
        String pre = elem.substring(0, separatorOffset);
        String post = elem.substring(separatorOffset + 1);

        if (post.startsWith("\"")) {
            post = post.substring(1, post.length() - 1);
        }

        if (post.endsWith("/")) {
            post = post.substring(0, post.length() - 1);
        } else if (post.endsWith("//*")) {
            post = post.substring(0, post.length() - 2) + "*";
        } else if (post.endsWith("/*")) {
            if (!isParentPath) {
                pre = "PARENTPATH";
                post = post.substring(0, post.length() - 2);
            }
        }

        if (post.startsWith("//")) {
            post = "*" + post.substring(1);
        }

        String[] nodes = post.split("/");
        post = Arrays.stream(nodes)
            .map(node -> node.contains(":") ? resolveToQName(node).toString() : node)
            .collect(Collectors.joining("/"))
            .replace("/", "\\/")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace(":", "\\:");
        return String.format("%s:%s", pre, post);
    }

    private String convertWildcardFieldValue(String value) {
        String toReturn = value;
        if (value.startsWith("\"")) {
            toReturn = toReturn.replace("\"", "");
            toReturn = toReturn.replace(" ", "\\ ");
            toReturn = toReturn.replace(":", "\\:");
            toReturn = toReturn.replace("/", "\\/");
            toReturn = toReturn.replace("-", "\\-");
            toReturn = toReturn.replace("+", "\\+");
            toReturn = toReturn.replace("!", "\\!");
        }
        return toReturn;
    }

    private String convertFieldName(String elem, int separatorOffset) {
        String resolved = resolveToQName(elem.substring(elem.indexOf("@") + 1, elem.indexOf("\\:")) + ":test")
            .getNamespaceURI();
        String replace = "\\{" + resolved.replace(":", "\\:") + "\\}";
        String toReturn = elem.substring(0, elem.indexOf("@") + 1).replace("@", "\\@") + replace
            + elem.substring(elem.indexOf("\\:") + 2, separatorOffset);
        return toReturn;
    }

    private QName resolveToQName(String name) {
        PrefixedQName qname = PrefixedQName.valueOf(name);
        String s = modelService
            .getNamespaceURI(qname.getNamespaceURI())
            .orElseThrow(() -> new RuntimeException("QName resolution exception (namespacePrefixResolver error): " + name));
        return new QName(s, qname.getLocalPart());
    }

    private String convertFieldValue(String elem, int separator) {
        String toReturn = elem.substring(separator + 1);
        // TODO: Controllare che in uscita vengano rispettate le virgolette
        if (toReturn.startsWith("\"")) {
            toReturn = toReturn.substring(1, toReturn.length() - 1);
        } else {
            toReturn = toReturn.replace("\\", "");
        }
        toReturn = "\"" + resolveToQName(toReturn) + "\"";
        return toReturn;
    }

    private String generateResult(SolrQuery query) {
        int counter = 0;
        char[] originalQuery = query.getOriginalQuery();
        StringBuffer toReturn = new StringBuffer();
        for (SolrQueryElement element : query.getElements()) {
            toReturn.append(new String(originalQuery, counter, element.getStart() - counter));
            toReturn.append(element.getConversion());
            counter = element.getEnd() + 1;
        }
        toReturn.append(new String(originalQuery, counter, originalQuery.length - counter));
        return toReturn.toString();
    }

    private String formatDate(Date date) {
        return ISO8601DateFormat.formatZ(date);
    }
}
