package com.poozh.translator.data;

import androidx.core.view.PointerIconCompat;
import com.poozh.translator.model.AnalysisResult;
import com.poozh.translator.model.LanguageDetector;
import com.poozh.translator.model.TermNote;
import com.poozh.translator.model.TextLanguage;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;
import org.json.JSONArray;
import org.json.JSONObject;

/* compiled from: AnalysisJsonParser.kt */
@Metadata(d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\bÆ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0016\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\u0007J\u0016\u0010\t\u001a\u00020\u00052\u0006\u0010\n\u001a\u00020\u00072\u0006\u0010\u000b\u001a\u00020\u0007J\u0010\u0010\f\u001a\u00020\u00072\u0006\u0010\u0006\u001a\u00020\u0007H\u0002J\u0018\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00072\u0006\u0010\n\u001a\u00020\u0007H\u0002J\u0014\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u0011*\u0004\u0018\u00010\u0013H\u0002J\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00070\u0011*\u0004\u0018\u00010\u0013H\u0002¨\u0006\u0015"}, d2 = {"Lcom/poozh/translator/data/AnalysisJsonParser;", "", "<init>", "()V", "parse", "Lcom/poozh/translator/model/AnalysisResult;", "raw", "", "fallbackSource", "fallback", "source", "message", "extractJsonObject", "parseLanguage", "Lcom/poozh/translator/model/TextLanguage;", "value", "toTermNotes", "", "Lcom/poozh/translator/model/TermNote;", "Lorg/json/JSONArray;", "toStringList", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes6.dex */
public final class AnalysisJsonParser {
    public static final AnalysisJsonParser INSTANCE = new AnalysisJsonParser();

    private AnalysisJsonParser() {
    }

    public final AnalysisResult parse(String raw, String fallbackSource) {
        Intrinsics.checkNotNullParameter(raw, "raw");
        Intrinsics.checkNotNullParameter(fallbackSource, "fallbackSource");
        String jsonText = extractJsonObject(raw);
        JSONObject json = new JSONObject(jsonText);
        String optString = json.optString("sourceText");
        if (StringsKt.isBlank(optString)) {
            optString = fallbackSource;
        }
        String source = optString;
        String optString2 = json.optString("language");
        Intrinsics.checkNotNullExpressionValue(optString2, "optString(...)");
        Intrinsics.checkNotNull(source);
        TextLanguage language = parseLanguage(optString2, source);
        String optString3 = json.optString("translation");
        Intrinsics.checkNotNullExpressionValue(optString3, "optString(...)");
        String optString4 = json.optString("summary");
        Intrinsics.checkNotNullExpressionValue(optString4, "optString(...)");
        List<TermNote> termNotes = toTermNotes(json.optJSONArray("vocabulary"));
        List<TermNote> termNotes2 = toTermNotes(json.optJSONArray("particles"));
        List<TermNote> termNotes3 = toTermNotes(json.optJSONArray("conjugations"));
        List<TermNote> termNotes4 = toTermNotes(json.optJSONArray("fixedExpressions"));
        String optString5 = json.optString("tone");
        Intrinsics.checkNotNullExpressionValue(optString5, "optString(...)");
        return new AnalysisResult(source, optString3, language, optString4, termNotes, termNotes2, termNotes3, termNotes4, optString5, toStringList(json.optJSONArray("grammar")));
    }

    public final AnalysisResult fallback(String source, String message) {
        Intrinsics.checkNotNullParameter(source, "source");
        Intrinsics.checkNotNullParameter(message, "message");
        return new AnalysisResult(source, "", LanguageDetector.INSTANCE.detect(source), message, null, null, null, null, null, null, PointerIconCompat.TYPE_TEXT, null);
    }

    private final String extractJsonObject(String raw) {
        String trimmed = StringsKt.trim((CharSequence) StringsKt.removeSuffix(StringsKt.removePrefix(StringsKt.removePrefix(StringsKt.trim((CharSequence) raw).toString(), (CharSequence) "```json"), (CharSequence) "```"), (CharSequence) "```")).toString();
        int start = StringsKt.indexOf$default((CharSequence) trimmed, '{', 0, false, 6, (Object) null);
        int end = StringsKt.lastIndexOf$default((CharSequence) trimmed, '}', 0, false, 6, (Object) null);
        if (!(start >= 0 && end > start)) {
            throw new IllegalArgumentException("DeepSeek 返回内容不是 JSON 对象".toString());
        }
        String substring = trimmed.substring(start, end + 1);
        Intrinsics.checkNotNullExpressionValue(substring, "substring(...)");
        return substring;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:11:?, code lost:
    
        return com.poozh.translator.model.TextLanguage.ENGLISH;
     */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x002b, code lost:
    
        if (r0.equals("日语") == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:15:0x0034, code lost:
    
        if (r0.equals("jp") == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:17:0x003d, code lost:
    
        if (r0.equals("ja") == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0046, code lost:
    
        if (r0.equals("en") == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x004f, code lost:
    
        if (r0.equals("japanese") == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:23:0x005b, code lost:
    
        if (r0.equals("english") == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:4:0x0019, code lost:
    
        if (r0.equals("日本語") == false) goto L31;
     */
    /* JADX WARN: Code restructure failed: missing block: B:6:?, code lost:
    
        return com.poozh.translator.model.TextLanguage.JAPANESE;
     */
    /* JADX WARN: Code restructure failed: missing block: B:9:0x0022, code lost:
    
        if (r0.equals("英语") == false) goto L31;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private final com.poozh.translator.model.TextLanguage parseLanguage(java.lang.String r3, java.lang.String r4) {
        /*
            r2 = this;
            java.util.Locale r0 = java.util.Locale.ROOT
            java.lang.String r0 = r3.toLowerCase(r0)
            java.lang.String r1 = "toLowerCase(...)"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            int r1 = r0.hashCode()
            switch(r1) {
                case -1603757456: goto L55;
                case -752730191: goto L49;
                case 3241: goto L40;
                case 3383: goto L37;
                case 3398: goto L2e;
                case 844456: goto L25;
                case 1074972: goto L1c;
                case 25921943: goto L13;
                default: goto L12;
            }
        L12:
            goto L61
        L13:
            java.lang.String r1 = "日本語"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L52
            goto L12
        L1c:
            java.lang.String r1 = "英语"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L5e
            goto L12
        L25:
            java.lang.String r1 = "日语"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L52
            goto L12
        L2e:
            java.lang.String r1 = "jp"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L52
            goto L12
        L37:
            java.lang.String r1 = "ja"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L52
            goto L12
        L40:
            java.lang.String r1 = "en"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L5e
            goto L12
        L49:
            java.lang.String r1 = "japanese"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L52
            goto L12
        L52:
            com.poozh.translator.model.TextLanguage r0 = com.poozh.translator.model.TextLanguage.JAPANESE
            goto L67
        L55:
            java.lang.String r1 = "english"
            boolean r0 = r0.equals(r1)
            if (r0 != 0) goto L5e
            goto L12
        L5e:
            com.poozh.translator.model.TextLanguage r0 = com.poozh.translator.model.TextLanguage.ENGLISH
            goto L67
        L61:
            com.poozh.translator.model.LanguageDetector r0 = com.poozh.translator.model.LanguageDetector.INSTANCE
            com.poozh.translator.model.TextLanguage r0 = r0.detect(r4)
        L67:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.poozh.translator.data.AnalysisJsonParser.parseLanguage(java.lang.String, java.lang.String):com.poozh.translator.model.TextLanguage");
    }

    private final List<TermNote> toTermNotes(JSONArray $this$toTermNotes) {
        if ($this$toTermNotes == null) {
            return CollectionsKt.emptyList();
        }
        List createListBuilder = CollectionsKt.createListBuilder();
        int length = $this$toTermNotes.length();
        for (int i = 0; i < length; i++) {
            JSONObject optJSONObject = $this$toTermNotes.optJSONObject(i);
            if (optJSONObject != null) {
                String optString = optJSONObject.optString("surface");
                Intrinsics.checkNotNull(optString);
                if (!StringsKt.isBlank(optString)) {
                    String optString2 = optJSONObject.optString("reading");
                    Intrinsics.checkNotNullExpressionValue(optString2, "optString(...)");
                    String optString3 = optJSONObject.optString("meaning");
                    Intrinsics.checkNotNullExpressionValue(optString3, "optString(...)");
                    String optString4 = optJSONObject.optString("note");
                    Intrinsics.checkNotNullExpressionValue(optString4, "optString(...)");
                    createListBuilder.add(new TermNote(optString, optString2, optString3, optString4));
                }
            }
        }
        return CollectionsKt.build(createListBuilder);
    }

    private final List<String> toStringList(JSONArray $this$toStringList) {
        if ($this$toStringList == null) {
            return CollectionsKt.emptyList();
        }
        List createListBuilder = CollectionsKt.createListBuilder();
        int length = $this$toStringList.length();
        for (int i = 0; i < length; i++) {
            String optString = $this$toStringList.optString(i);
            Intrinsics.checkNotNull(optString);
            if (!StringsKt.isBlank(optString)) {
                createListBuilder.add(optString);
            }
        }
        return CollectionsKt.build(createListBuilder);
    }
}
