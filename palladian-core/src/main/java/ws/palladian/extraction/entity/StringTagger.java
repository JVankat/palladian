package ws.palladian.extraction.entity;

import java.util.regex.Pattern;

/**
 * <p>
 * Tag possible named entities in an English text.
 * </p>
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class StringTagger extends RegExTagger {
    public static final String CANDIDATE_TAG = "CANDIDATE";

    private static final Pattern PATTERN = compilePattern();

    public static final StringTagger INSTANCE = new StringTagger();

    private StringTagger() {
        super(StringTagger.PATTERN, CANDIDATE_TAG);
    }

    private static Pattern compilePattern() {
        String regexp = "";

        String camelCaseWords = "(GmbH|LLC)";
        String suffixes = "((?<=(Inc|Corp|Co|Ave))\\.)?";

        // dashes (such as "Ontario-based" "Victor" or St. Louis-based)
        regexp += "([A-Z][a-z]\\. )?([A-Z]{1}[A-Za-z\\p{Ll}]+(-[a-z\\p{Ll}]+)(-[A-Za-z\\p{Ll}]+)*)";
        regexp += "|";

        // names
        // A. Anderson
        regexp += "([A-Z]\\.)( )?[A-Z]{1}['’A-Za-z\\p{Ll}]{1,100}";
        regexp += "|";
        // Alexander A. Anderson, Mayor Bobby E. Horton
        regexp += "([A-Z][a-z\\p{Ll}]+ ){1,2}[A-Z]{1}\\. [A-Za-z\\p{Ll}]{1,100}";
        regexp += "|";
        // Dr. Anderson Emeraldy
        regexp += "([A-Z][a-z\\p{Ll}]{0,2}\\.) [A-Z]{1}[A-Za-z\\p{Ll}]{1,100}( [A-Z]{1}[A-Za-z\\p{Ll}]{1,100})?";
        regexp += "|";
        // A.B.C. Anderson00 Anderson12 Emeraldy
        regexp += "([A-Z]\\.)+( ([A-Z]{1}([A-Za-z-\\p{Ll}0-9&]+))+(([ ])*[A-Z]+([A-Za-z-\\p{Ll}0-9]*)){0,10})*";

        // ending with dash (Real- Rumble => should be two words, TOTALLY FREE- Abc => also two matches)
        regexp += "|";
        regexp += "([A-Z][A-Za-z\\p{Ll}]+ )*[A-Z][A-Za-z\\p{Ll}]+(?=-+? )";

        // small with dash (ex-President)
        regexp += "|";
        regexp += "([A-Z][A-Za-z\\p{Ll}]+ )?([a-z\\p{Ll}]+-[A-Z][A-Za-z\\p{Ll}0-9]+)";

        // ___ of ___, ___ de ___
        // such as "National Bank of Scotland" or "Duke of South Carolina" or "L’Arc de Triomphe"
        // always in the form of X Y of Z OR X of Y Z
        regexp += "|";
        regexp += "(([A-Z]{1}['’]?[A-Za-z\\p{Ll}]+ )+(?:of|de) (([A-Z]{1}[A-Za-z-\\p{Ll}]+)(?!([a-z-]{0,20}\\s[A-Z]))))|([A-Z]{1}[A-Za-z-\\p{Ll}]+ of( [A-Z]{1}[A-Za-z\\p{Ll}]+){1,})";

        // prevent mixtures of mix camel cases => "Veronica Swenston VENICE" should be two matches
        regexp += "|";
        regexp += "([A-Z]{1}([a-z-\\p{Ll}0-9®]+)(( " + camelCaseWords + ")?(([ &])*([A-Z]['’])?[A-Z]{1}([a-z-\\p{Ll}0-9®]+))?)*)" + suffixes;

        // names (such as "O'Sullivan"), compounds such as "D&G"
        regexp += "|";
        regexp += "((([A-Z]{1}([A-Za-z-\\p{Ll}0-9&]+|['’][A-Z][A-Za-z]{2,20}))+(([ &])*[A-Z]+(['’][A-Z])?([A-Za-z-\\p{Ll}0-9®]*)){0,10})(?!(\\.[A-Z])+))" + suffixes;

        // camel case (iPhone 4)
        regexp += "|";
        regexp += "([a-z][A-Z][A-Za-z0-9]+( [A-Z0-9][A-Za-z0-9]{0,20}){0,20})";

        return Pattern.compile(regexp);
    }
}
