package tud.iir.extraction.mio;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tud.iir.helper.HTMLHelper;
import tud.iir.helper.MathHelper;
import tud.iir.knowledge.Entity;
import tud.iir.web.Crawler;

public class FlashExtractor extends AbstractMIOTypeExtractor {

    /** The mio type. */
    private static String mioType = "flash";

    /** The mio page. */
    private transient MIOPage mioPage = null;

    /** The entity. */
    private transient Entity entity = null;

    /** The modified mioPageContent (without relevant tags). */
    private transient String modMioPageContent;

    /** The reg exp. */
    private static String regExp = "(\".[^\",]*\\.swf\")|(\".[^\",]*\\.swf\\?.[^\"]*\")";

    private static String fVString = "flashvars";

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#extractMIOsByType(tud.iir.extraction.mio.MIOPage,
     * tud.iir.knowledge.Entity)
     */
    @Override
    List<MIO> extractMIOsByType(final MIOPage mioPage, final Entity entity) {

        this.mioPage = mioPage;
        this.entity = entity;

        final List<MIO> mioList = new ArrayList<MIO>();
        final List<String> relevantTags = extractRelevantTags(mioPage.getContentAsString());
        // check if there are swf-Files out of tags left, e.g. in comments
        mioList.addAll(findOutOfTagMIOs(modMioPageContent));

        mioList.addAll(analyzeRelevantTags(relevantTags));

        return mioList;

    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#extractRelevantTags(java.lang.String)
     */
    @Override
    final List<String> extractRelevantTags(final String mioPageContent) {

        modMioPageContent = "";

        final List<String> relevantTags = new ArrayList<String>();

        // extract all <object>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(mioPageContent, "object"));

        // remove the object-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(mioPageContent, "object");

        // extract all remaining <embed>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(modMioPageContent, "embed"));

        // remove all <embed>-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(modMioPageContent, "embed");

        // extract all <script>-tags
        relevantTags.addAll(HTMLHelper.getConcreteTags(modMioPageContent, "script"));

        // remove all <script>-tags
        modMioPageContent = HTMLHelper.removeConcreteHTMLTag(modMioPageContent, "script");

        return relevantTags;
    }

    /**
     * Find out of tag MIOs.
     * 
     * @param mioPageContent the MIOPageContent
     * @return the list
     */
    private List<MIO> findOutOfTagMIOs(final String mioPageContent) {
        final List<MIO> flashMIOs = new ArrayList<MIO>();

        if (mioPageContent.toLowerCase(Locale.ENGLISH).contains(".swf")) {

            final List<MIO> furtherSWFs = extractMioURL(mioPageContent, mioPage, "(\".[^\",;]*\\.swf\")|(\".[^\",;]*\\.swf\\?.[^\"]*\")", entity, mioType);
//             System.out.println("NOCH SWF ENTHALTEN! - " + mioPage.getUrl());
//             for (MIO mio : furtherSWFs) {
//             System.out.println(mio.getDirectURL());
//             }
            flashMIOs.addAll(furtherSWFs);

        }
        return flashMIOs;

    }

    /*
     * (non-Javadoc)
     * @see tud.iir.extraction.mio.MIOTypeExtractor#analyzeRelevantTags(java.util.List)
     */
    @Override
    List<MIO> analyzeRelevantTags(final List<String> relevantTags) {
        final List<MIO> retrievedMIOs = new ArrayList<MIO>();
        final List<MIO> tempMIOs = new ArrayList<MIO>();

//        final List<String> altText = new ArrayList<String>();
//        StringBuffer altTextBuffer;
        // try to extract swf-file-URLs
        for (String relevantTag : relevantTags) {

            tempMIOs.clear();
            if (relevantTag.toLowerCase(Locale.ENGLISH).contains("swfobject")) {
//                System.out.println(relevantTag);
                tempMIOs.addAll(checkSWFObject(relevantTag, mioPage));

            } else {
                // extract all swf-files from a relevant-tag
                tempMIOs.addAll(extractMioURL(relevantTag, mioPage, regExp, entity, mioType));

                // check for flashvars
                if (relevantTag.toLowerCase(Locale.ENGLISH).contains(fVString)) {

                    final List<String> flashVars = extractFlashVars(relevantTag);
                    if (!flashVars.isEmpty()) {
                        for (MIO mio : tempMIOs) {
                            mio = adaptFlashVarsToURL(mio, flashVars);
//                            mio.addInfos(fVString, flashVars);
                        }
                    }
                }
            }

            // extract ALT-Text from object and embed-tags and add to MIO-Infos
            if (!relevantTag.toLowerCase(Locale.ENGLISH).startsWith("<script")) {

                for (MIO mio : tempMIOs) {
                    final String tempAltText = extractALTTextFromTag(relevantTag);
//                    altText.clear();
                   
                    if (tempAltText.length() > 2) {
//                         altTextBuffer = new StringBuffer();
//                        altText.add(tempAltText);
                        mio.setAltText(tempAltText);
                    }
                }
            }
            // extract surrounding Information(Headlines, TextContent) and add to MIO-infos
            // final List<String> headlines = new ArrayList<String>();
            for (MIO mio : tempMIOs) {
                extractSurroundingInfo(relevantTag, mioPage, mio);
                extractXMLInfo(relevantTag, mio);
            }

            retrievedMIOs.addAll(tempMIOs);
        }

        return retrievedMIOs;
    }

    /**
     * Check swf object.
     * 
     * @param relevantTag the relevant tag
     * @param mioPage the mio page
     * @return the list
     */
    private List<MIO> checkSWFObject(final String relevantTag, final MIOPage mioPage) {

        List<MIO> tempList = new ArrayList<MIO>();

        // String content = mioPage.getContent();
        if (relevantTag.toLowerCase(Locale.ENGLISH).contains("swfobject.embedswf")
                || relevantTag.toLowerCase(Locale.ENGLISH).contains("new swfobject(")) {

            tempList = extractMioURL(relevantTag, mioPage, regExp, entity, mioType);

            // check for flashvars
            if (relevantTag.toLowerCase(Locale.ENGLISH).contains(fVString)) {
                final List<String> flashVars = extractFlashVars(relevantTag);
                // System.out.println(flashVars.toString());

                if (!flashVars.isEmpty()) {
                    for (MIO mio : tempList) {
                        mio = adaptFlashVarsToURL(mio, flashVars);
//                        mio.addInfos(fVString, flashVars);
                    }
                }
            }

            // analyze for queryParamValues
//            if (relevantTag.contains("getQueryParamValue")) {
//
//                String regExp = "getQueryParamValue\\(.[^\\)]*\\)";
//                // String queryParamValue= extractElement(regExp, relevantTag,
//                // "getQueryParamValue(");
//                // //remove the closing ")"
//                // queryParamValue = queryParamValue.substring(0,
//                // queryParamValue.length()-1);
//
//                final Pattern pat = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
//                final Matcher matcher = pat.matcher(relevantTag);
//                final List<String> queryParamValues = new ArrayList<String>();
//                while (matcher.find()) {
//                    // result has the form: flashvars = {cool};
//                    final String result = matcher.group(0);
//                    queryParamValues.add(result);
//
//                }
//                for (MIO mio : tempList) {
//                    mio.addInfos("queryParamValues", queryParamValues);
//                }
//
//            }

        }

        return tempList;
    }

    /**
     * Extract flash vars.
     * 
     * @param tagContent the tag content
     * @return the list
     */
    private List<String> extractFlashVars(final String tagContent) {
//        System.out.println(tagContent);
        final List<String> flashVars = new ArrayList<String>();

        // extract var flashVars = {}
        String regExp = "flashvars(\\s?)=(\\s?)\\{[^\\}]*\\};";
        final Pattern pat1 = Pattern.compile(regExp, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher matcher1 = pat1.matcher(tagContent);
        while (matcher1.find()) {
            // result has the form: flashvars = {cool};
            String result = matcher1.group(0);
//            System.out.println("#### " + result);
            result = result.replaceAll("flashvars(\\s?)=(\\s?)", "");
            result = result.trim();
            result = result.substring(0, result.length() - 1);
            result = result.replaceAll("[\\{,\\}]", "");
            result = result.trim();
//            System.out.println(result);
            // result has the form: cool
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }

        // extract <param name="FlashVars"
        // value="myURL=http://weblogs.adobe.com/">
//        String regExp2 = "<param[^>]*flashvars[^>]*>";
        String regExp2 = "param name=\"flashvars\"[^>]*value=\".[^\"]+\"";
        Pattern pat2 = Pattern.compile(regExp2, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher2 = pat2.matcher(tagContent);
        while (matcher2.find()) {
            // result has the form: <param name="FlashVars"
            // value="myURL=http://weblogs.adobe.com/">
            String result = matcher2.group();
//            System.out.println(result);
            String pattern ="";
            if (result.contains(";")){
                // if result has the comment or CDATA-form: flashObj = flashObj + '&lt;param name="flashVars"
                // value="xmlUrl=/us/consumer/detail/galleryXML.do?model_cd=CLP-770ND/XAA&amp;amp;disMod=L&amp;"
                pattern = "value=\".[^\"]*";
            }else{
           
                 pattern = "value=\"[^>]*\"";
            }
           
            // System.out.println("---------" + result);
            result = HTMLHelper.extractTagElement(pattern, result, "value=");
            if (result.length() > 0) {
                result.replaceAll(";", "");
                flashVars.add(result);
            }

        }

        // extract FlashVars="myURL=http://weblogs.adobe.com/"
        String regExp3 = "flashvars=\"[^\"]*\"";
        Pattern pat3 = Pattern.compile(regExp3, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        Matcher matcher3 = pat3.matcher(tagContent);
        while (matcher3.find()) {
            String result = matcher3.group(0);
//            System.out.println(result);
            String pattern = "flashvars=\"[^\"]*\"";
            result = HTMLHelper.extractTagElement(pattern, result, "flashvars=");
            if (result.length() > 0) {
                flashVars.add(result);
            }
        }

        // extract flashvars.country = "us";
        String regExp4 = "flashvars\\..[^;]*;";
        final Pattern pat4 = Pattern.compile(regExp4, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
        final Matcher matcher4 = pat4.matcher(tagContent);
        while (matcher4.find()) {
            String result = matcher4.group(0);
//            System.out.println("--" + result);
            result = result.replaceFirst("flashvars.", "");
            result = result.replaceAll("\"", "");
            result = result.replaceAll(";", "");
            result = result.replaceAll("\\s", "");
            if (result.length() > 0) {
                flashVars.add(result);
//                System.out.println(result);
            }
        }

        return flashVars;
    }

    /**
     * Adapt flash vars to url.
     * 
     * @param mio the mio
     * @param flashVars the flash vars
     * @return the mIO
     */
    private MIO adaptFlashVarsToURL(final MIO mio, final List<String> flashVars) {

        final String url = mio.getDirectURL();

        final StringBuffer modURL = new StringBuffer();
        // Prepare directURL for adding flashVars
        modURL.append(url + "?");
        // add each flashVar
        for (String flashVar : flashVars) {
//            System.out.println(flashVar);
            // only concate not existing flashVars
            if (!modURL.toString().contains(flashVar)) {
                if (!flashVar.contains("\"")) {
                  
                        final String tempURL = flashVar+"&";
                        modURL.append(tempURL);
                    
                } else {
                    // adapt vars of style:
                    // videoRef:"/uk/assets/cms/6d8a4d0c-9659-4fa0-a62c-e884980879ee/Video.flv?p=081007_09:24"
                    String tempURL = flashVar.replaceAll("\"", "");
                    tempURL = tempURL.replaceFirst(":", "=");
                    modURL.append(tempURL + "&");

                }
            }

        }
        if (Crawler.isValidURL(modURL.toString(), false)) {
            mio.setDirectURL(modURL.toString());
//            System.out.println(modURL.toString());
        }

        return mio;
    }

    // private List<MIO> extractSWFFromComments(MIOPage mioPage) {
    // List<MIO> resultList = new ArrayList<MIO>();
    // // StringHelper stringHelper = new StringHelper();
    // List<String> relevantTags = StringHelper.getConcreteTags(mioPage.getContent(), "<!--", "-->");
    // for (String relevantTag : relevantTags) {
    //
    // List<MIO> tempList = extractMIOWithURL(relevantTag, mioPage, "flash");
    // if (relevantTag.contains("flashvars")) {
    // List flashVars = extractFlashVars(relevantTag);
    // if (!flashVars.isEmpty()) {
    // for (MIO mio : tempList) {
    // mio.addInfos("flashvars", flashVars);
    // }
    // }
    //
    // }
    // resultList.addAll(tempList);
    //
    // }
    // return resultList;
    // }
    //
    // /**
    // * The main method.
    // *
    // * @param args the arguments
    // */

    public static void main(final String[] abc) {
        
//        System.out.println(MathHelper.calculateRMSE("F:/rmse.csv", ","));
//        System.exit(1);

//        MIOPage mioPage = new MIOPage("http://www.samsung.com/us/consumer/office/printers-multifunction/color-laser-printers/CLP-770ND/XAA/index.idx?pagetype=prd_detail");
//        Entity entity = new Entity("Samsung CLP-770ND");
      MIOPage mioPage = new MIOPage("http://beepdf.com/doc/16/hp_printer_a4_mobile_officejet_h470_iy318.html");
      Entity entity = new Entity("HP Officejet H470");
        FlashExtractor flashEx = new FlashExtractor();
       List<MIO> mios =  flashEx.extractMIOsByType(mioPage, entity);
       for(MIO mio:mios){
           System.out.println(mio.getDirectURL());
       }
    }

}
