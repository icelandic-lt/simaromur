package com.grammatek.simaromur.frontend;

import android.content.Context;
import android.util.Log;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The NormalizationManager controls the normalization process from raw input text to
 * normalized text. It contains:
 *      - a pre-normalization step to clean unicode, i.e. reduce the number of
 *        characters by deleting irrelevant characters and reducing similar characters to one
 *        (e.g. dash and hyphen variations to hypen-minus (\u202d or 45 decimal))
 *      - a tokenizing step
 *      - the core normalization step composed of pre-normalization, pos-tagging and
 *      post-normalization
 */

public class NormalizationManager {
    private final static boolean DEBUG = false;
    private final static String LOG_TAG = "Simaromur_Java_" + NormalizationManager.class.getSimpleName();
    private static final String POS_MODEL = "pos/is-pos-reduced-maxent.bin";

    private final Context mContext;
    private POSTaggerME mPosTagger;
    private final TTSUnicodeNormalizer mUnicodeNormalizer;
    private final Tokenizer mTokenizer;
    private final TTSNormalizer mTTSNormalizer;

    public NormalizationManager(Context context, Map<String, PronDictEntry> pronDict) {
        mContext = context;
        mUnicodeNormalizer = new TTSUnicodeNormalizer(context, pronDict);
        mTokenizer = new Tokenizer(context);
        mTTSNormalizer = new TTSNormalizer();
    }

    /**
     * Processes the input text according to the defined steps: unicode cleaning,
     * tokenizing, normalizing
     * @param text the input text
     * @param doIgnoreUserDict if true, the user dictionary is ignored
     * @return normalized version of 'text'
     */
    public String process(final String text, boolean doIgnoreUserDict) {
        Log.v(LOG_TAG, "process() called");
        String cleaned = mUnicodeNormalizer.normalizeEncoding(text);

        List<String> strings = mTokenizer.detectSentences(cleaned);
        List<String> normalizedSentences = normalize(strings, doIgnoreUserDict);
        List<String> cleanNormalized = mUnicodeNormalizer.normalizeAlphabet(normalizedSentences);
        for (String sentence : cleanNormalized) {
            Log.v(LOG_TAG, "normalized sentence: " + sentence);
        }
        return list2string(cleanNormalized);
    }

    // pre-normalization, tagging and final normalization of the sentences in 'tokenized'
    private List<String> normalize(final List<String> strings, boolean doIgnoreUserDict) {
        String preNormalized;
        List<String> normalized = new ArrayList<>();

        for (String sentence : strings) {
            preNormalized = mTTSNormalizer.preNormalize(sentence, doIgnoreUserDict);
            String[] tags = tagText(preNormalized);
            // preNormalized is tokenized as string, so we know splitting on whitespace will give
            // us the correct tokens according to the tokenizer
            String postNormalized = mTTSNormalizer.postNormalize(preNormalized.split("\\s+"), tags);
            normalized.add(postNormalized);
        }

        return normalized;
    }

    private String list2string(final List<String> normalizedSentences) {
        StringBuilder sb = new StringBuilder();
        for (String sentence : normalizedSentences) {
            sb.append(" ");
            sb.append(sentence);
        }
        return sb.toString().trim();
    }

    /**
     * Tags the text with POS tags. If there are no numbers in the text, we can skip the
     * pos-tagging and return a list of dummy tags that just contain "§" for each token.
     *
     * @param text  the text to be tagged
     * @return      the tags for each token in the text
     */
    private String[] tagText(final String text) {
        String[] tags;
        String[] tokens = text.split("\\s+");

        // optimization: if there are no numbers in the text, we can skip the pos-tagging
        // and return a list of dummy tags that just contain "§" for each token
        if (! text.matches(".*\\d.*")) {
            tags = new String[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                tags[i] = "§";
            }

            return tags;
        }

        if (mPosTagger == null) {
            // this takes ~2 seconds
            mPosTagger = initPOSTagger();
            if (mPosTagger == null) {
                Log.e(LOG_TAG, "Failed to initialize POS tagger");
                throw new RuntimeException("Failed to initialize POS tagger");
            }
        }
        tags = mPosTagger.tag(tokens);
        if (DEBUG) {
            printProbabilities(tags, mPosTagger, tokens);
        }
        return tags;
    }

    private POSTaggerME initPOSTagger() {
        POSTaggerME posTagger = null;
        try {
            // read model from assets
            InputStream iStream = mContext.getAssets().open(POS_MODEL);
            POSModel posModel = new POSModel(iStream);
            posTagger = new POSTaggerME(posModel);
        } catch(IOException e) {
            e.printStackTrace();
        }
        return posTagger;
    }

    // Get the probabilities of the tags given to the tokens to inspect
    private void printProbabilities(String[] tags, POSTaggerME posTagger, String[] tokens) {
        double[] probs = posTagger.probs();
        Log.v(LOG_TAG, "Token\t:\tTag\t:\tProbability\n--------------------------");
        for(int i=0;i<tokens.length;i++){
            Log.v(LOG_TAG, tokens[i]+"\t:\t"+tags[i]+"\t:\t"+probs[i]);
        }
    }
}

