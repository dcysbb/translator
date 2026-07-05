package com.poozh.translator.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* compiled from: AnalysisResult.kt */
@Metadata(d1 = {"\u0000H\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0016\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0083\u0001\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t\u0012\u000e\b\u0002\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\n0\t\u0012\u000e\b\u0002\u0010\f\u001a\b\u0012\u0004\u0012\u00020\n0\t\u0012\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\n0\t\u0012\b\b\u0002\u0010\u000e\u001a\u00020\u0003\u0012\u000e\b\u0002\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00030\t¢\u0006\u0004\b\u0010\u0010\u0011J\u0006\u0010\u001f\u001a\u00020\u0003J*\u0010 \u001a\u00020!2\n\u0010\"\u001a\u00060#j\u0002`$2\u0006\u0010%\u001a\u00020\u00032\f\u0010&\u001a\b\u0012\u0004\u0012\u00020\n0\tH\u0002J\t\u0010'\u001a\u00020\u0003HÆ\u0003J\t\u0010(\u001a\u00020\u0003HÆ\u0003J\t\u0010)\u001a\u00020\u0006HÆ\u0003J\t\u0010*\u001a\u00020\u0003HÆ\u0003J\u000f\u0010+\u001a\b\u0012\u0004\u0012\u00020\n0\tHÆ\u0003J\u000f\u0010,\u001a\b\u0012\u0004\u0012\u00020\n0\tHÆ\u0003J\u000f\u0010-\u001a\b\u0012\u0004\u0012\u00020\n0\tHÆ\u0003J\u000f\u0010.\u001a\b\u0012\u0004\u0012\u00020\n0\tHÆ\u0003J\t\u0010/\u001a\u00020\u0003HÆ\u0003J\u000f\u00100\u001a\b\u0012\u0004\u0012\u00020\u00030\tHÆ\u0003J\u008b\u0001\u00101\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00032\u000e\b\u0002\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t2\u000e\b\u0002\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\n0\t2\u000e\b\u0002\u0010\f\u001a\b\u0012\u0004\u0012\u00020\n0\t2\u000e\b\u0002\u0010\r\u001a\b\u0012\u0004\u0012\u00020\n0\t2\b\b\u0002\u0010\u000e\u001a\u00020\u00032\u000e\b\u0002\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00030\tHÆ\u0001J\u0013\u00102\u001a\u0002032\b\u00104\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u00105\u001a\u000206HÖ\u0001J\t\u00107\u001a\u00020\u0003HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0013R\u0011\u0010\u0005\u001a\u00020\u0006¢\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0007\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0013R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\t¢\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0017\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\n0\t¢\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0019R\u0017\u0010\f\u001a\b\u0012\u0004\u0012\u00020\n0\t¢\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u0019R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\n0\t¢\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u0019R\u0011\u0010\u000e\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u0013R\u0017\u0010\u000f\u001a\b\u0012\u0004\u0012\u00020\u00030\t¢\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u0019¨\u00068"}, d2 = {"Lcom/poozh/translator/model/AnalysisResult;", "", "sourceText", "", "translation", "language", "Lcom/poozh/translator/model/TextLanguage;", "summary", "vocabulary", "", "Lcom/poozh/translator/model/TermNote;", "particles", "conjugations", "fixedExpressions", "tone", "grammar", "<init>", "(Ljava/lang/String;Ljava/lang/String;Lcom/poozh/translator/model/TextLanguage;Ljava/lang/String;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/lang/String;Ljava/util/List;)V", "getSourceText", "()Ljava/lang/String;", "getTranslation", "getLanguage", "()Lcom/poozh/translator/model/TextLanguage;", "getSummary", "getVocabulary", "()Ljava/util/List;", "getParticles", "getConjugations", "getFixedExpressions", "getTone", "getGrammar", "toDisplayText", "appendNotes", "", "builder", "Ljava/lang/StringBuilder;", "Lkotlin/text/StringBuilder;", "title", "notes", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "component10", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes7.dex */
public final /* data */ class AnalysisResult {
    private final List<TermNote> conjugations;
    private final List<TermNote> fixedExpressions;
    private final List<String> grammar;
    private final TextLanguage language;
    private final List<TermNote> particles;
    private final String sourceText;
    private final String summary;
    private final String tone;
    private final String translation;
    private final List<TermNote> vocabulary;

    public static /* synthetic */ AnalysisResult copy$default(AnalysisResult analysisResult, String str, String str2, TextLanguage textLanguage, String str3, List list, List list2, List list3, List list4, String str4, List list5, int i, Object obj) {
        if ((i & 1) != 0) {
            str = analysisResult.sourceText;
        }
        if ((i & 2) != 0) {
            str2 = analysisResult.translation;
        }
        if ((i & 4) != 0) {
            textLanguage = analysisResult.language;
        }
        if ((i & 8) != 0) {
            str3 = analysisResult.summary;
        }
        if ((i & 16) != 0) {
            list = analysisResult.vocabulary;
        }
        if ((i & 32) != 0) {
            list2 = analysisResult.particles;
        }
        if ((i & 64) != 0) {
            list3 = analysisResult.conjugations;
        }
        if ((i & 128) != 0) {
            list4 = analysisResult.fixedExpressions;
        }
        if ((i & 256) != 0) {
            str4 = analysisResult.tone;
        }
        if ((i & 512) != 0) {
            list5 = analysisResult.grammar;
        }
        String str5 = str4;
        List list6 = list5;
        List list7 = list3;
        List list8 = list4;
        List list9 = list;
        List list10 = list2;
        return analysisResult.copy(str, str2, textLanguage, str3, list9, list10, list7, list8, str5, list6);
    }

    /* renamed from: component1, reason: from getter */
    public final String getSourceText() {
        return this.sourceText;
    }

    public final List<String> component10() {
        return this.grammar;
    }

    /* renamed from: component2, reason: from getter */
    public final String getTranslation() {
        return this.translation;
    }

    /* renamed from: component3, reason: from getter */
    public final TextLanguage getLanguage() {
        return this.language;
    }

    /* renamed from: component4, reason: from getter */
    public final String getSummary() {
        return this.summary;
    }

    public final List<TermNote> component5() {
        return this.vocabulary;
    }

    public final List<TermNote> component6() {
        return this.particles;
    }

    public final List<TermNote> component7() {
        return this.conjugations;
    }

    public final List<TermNote> component8() {
        return this.fixedExpressions;
    }

    /* renamed from: component9, reason: from getter */
    public final String getTone() {
        return this.tone;
    }

    public final AnalysisResult copy(String sourceText, String translation, TextLanguage language, String summary, List<TermNote> vocabulary, List<TermNote> particles, List<TermNote> conjugations, List<TermNote> fixedExpressions, String tone, List<String> grammar) {
        Intrinsics.checkNotNullParameter(sourceText, "sourceText");
        Intrinsics.checkNotNullParameter(translation, "translation");
        Intrinsics.checkNotNullParameter(language, "language");
        Intrinsics.checkNotNullParameter(summary, "summary");
        Intrinsics.checkNotNullParameter(vocabulary, "vocabulary");
        Intrinsics.checkNotNullParameter(particles, "particles");
        Intrinsics.checkNotNullParameter(conjugations, "conjugations");
        Intrinsics.checkNotNullParameter(fixedExpressions, "fixedExpressions");
        Intrinsics.checkNotNullParameter(tone, "tone");
        Intrinsics.checkNotNullParameter(grammar, "grammar");
        return new AnalysisResult(sourceText, translation, language, summary, vocabulary, particles, conjugations, fixedExpressions, tone, grammar);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AnalysisResult)) {
            return false;
        }
        AnalysisResult analysisResult = (AnalysisResult) other;
        return Intrinsics.areEqual(this.sourceText, analysisResult.sourceText) && Intrinsics.areEqual(this.translation, analysisResult.translation) && this.language == analysisResult.language && Intrinsics.areEqual(this.summary, analysisResult.summary) && Intrinsics.areEqual(this.vocabulary, analysisResult.vocabulary) && Intrinsics.areEqual(this.particles, analysisResult.particles) && Intrinsics.areEqual(this.conjugations, analysisResult.conjugations) && Intrinsics.areEqual(this.fixedExpressions, analysisResult.fixedExpressions) && Intrinsics.areEqual(this.tone, analysisResult.tone) && Intrinsics.areEqual(this.grammar, analysisResult.grammar);
    }

    public int hashCode() {
        return (((((((((((((((((this.sourceText.hashCode() * 31) + this.translation.hashCode()) * 31) + this.language.hashCode()) * 31) + this.summary.hashCode()) * 31) + this.vocabulary.hashCode()) * 31) + this.particles.hashCode()) * 31) + this.conjugations.hashCode()) * 31) + this.fixedExpressions.hashCode()) * 31) + this.tone.hashCode()) * 31) + this.grammar.hashCode();
    }

    public String toString() {
        return "AnalysisResult(sourceText=" + this.sourceText + ", translation=" + this.translation + ", language=" + this.language + ", summary=" + this.summary + ", vocabulary=" + this.vocabulary + ", particles=" + this.particles + ", conjugations=" + this.conjugations + ", fixedExpressions=" + this.fixedExpressions + ", tone=" + this.tone + ", grammar=" + this.grammar + ")";
    }

    public AnalysisResult(String sourceText, String translation, TextLanguage language, String summary, List<TermNote> vocabulary, List<TermNote> particles, List<TermNote> conjugations, List<TermNote> fixedExpressions, String tone, List<String> grammar) {
        Intrinsics.checkNotNullParameter(sourceText, "sourceText");
        Intrinsics.checkNotNullParameter(translation, "translation");
        Intrinsics.checkNotNullParameter(language, "language");
        Intrinsics.checkNotNullParameter(summary, "summary");
        Intrinsics.checkNotNullParameter(vocabulary, "vocabulary");
        Intrinsics.checkNotNullParameter(particles, "particles");
        Intrinsics.checkNotNullParameter(conjugations, "conjugations");
        Intrinsics.checkNotNullParameter(fixedExpressions, "fixedExpressions");
        Intrinsics.checkNotNullParameter(tone, "tone");
        Intrinsics.checkNotNullParameter(grammar, "grammar");
        this.sourceText = sourceText;
        this.translation = translation;
        this.language = language;
        this.summary = summary;
        this.vocabulary = vocabulary;
        this.particles = particles;
        this.conjugations = conjugations;
        this.fixedExpressions = fixedExpressions;
        this.tone = tone;
        this.grammar = grammar;
    }

    public /* synthetic */ AnalysisResult(String str, String str2, TextLanguage textLanguage, String str3, List list, List list2, List list3, List list4, String str4, List list5, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, str2, textLanguage, (i & 8) != 0 ? "" : str3, (i & 16) != 0 ? CollectionsKt.emptyList() : list, (i & 32) != 0 ? CollectionsKt.emptyList() : list2, (i & 64) != 0 ? CollectionsKt.emptyList() : list3, (i & 128) != 0 ? CollectionsKt.emptyList() : list4, (i & 256) != 0 ? "" : str4, (i & 512) != 0 ? CollectionsKt.emptyList() : list5);
    }

    public final String getSourceText() {
        return this.sourceText;
    }

    public final String getTranslation() {
        return this.translation;
    }

    public final TextLanguage getLanguage() {
        return this.language;
    }

    public final String getSummary() {
        return this.summary;
    }

    public final List<TermNote> getVocabulary() {
        return this.vocabulary;
    }

    public final List<TermNote> getParticles() {
        return this.particles;
    }

    public final List<TermNote> getConjugations() {
        return this.conjugations;
    }

    public final List<TermNote> getFixedExpressions() {
        return this.fixedExpressions;
    }

    public final String getTone() {
        return this.tone;
    }

    public final List<String> getGrammar() {
        return this.grammar;
    }

    public final String toDisplayText() {
        StringBuilder builder = new StringBuilder();
        StringBuilder append = builder.append("原文");
        Intrinsics.checkNotNullExpressionValue(append, "append(...)");
        Intrinsics.checkNotNullExpressionValue(append.append('\n'), "append(...)");
        String str = this.sourceText;
        if (StringsKt.isBlank(str)) {
            str = "未识别到文本";
        }
        StringBuilder append2 = builder.append(str);
        Intrinsics.checkNotNullExpressionValue(append2, "append(...)");
        Intrinsics.checkNotNullExpressionValue(append2.append('\n'), "append(...)");
        Intrinsics.checkNotNullExpressionValue(builder.append('\n'), "append(...)");
        StringBuilder append3 = builder.append("中文");
        Intrinsics.checkNotNullExpressionValue(append3, "append(...)");
        Intrinsics.checkNotNullExpressionValue(append3.append('\n'), "append(...)");
        String str2 = this.translation;
        if (StringsKt.isBlank(str2)) {
            str2 = "暂无翻译";
        }
        StringBuilder append4 = builder.append(str2);
        Intrinsics.checkNotNullExpressionValue(append4, "append(...)");
        Intrinsics.checkNotNullExpressionValue(append4.append('\n'), "append(...)");
        if (!StringsKt.isBlank(this.summary)) {
            Intrinsics.checkNotNullExpressionValue(builder.append('\n'), "append(...)");
            StringBuilder append5 = builder.append("说明");
            Intrinsics.checkNotNullExpressionValue(append5, "append(...)");
            Intrinsics.checkNotNullExpressionValue(append5.append('\n'), "append(...)");
            StringBuilder append6 = builder.append(this.summary);
            Intrinsics.checkNotNullExpressionValue(append6, "append(...)");
            Intrinsics.checkNotNullExpressionValue(append6.append('\n'), "append(...)");
        }
        appendNotes(builder, "词汇", this.vocabulary);
        appendNotes(builder, "助词", this.particles);
        appendNotes(builder, "活用", this.conjugations);
        appendNotes(builder, "固定表达", this.fixedExpressions);
        if (!StringsKt.isBlank(this.tone)) {
            Intrinsics.checkNotNullExpressionValue(builder.append('\n'), "append(...)");
            StringBuilder append7 = builder.append("语气");
            Intrinsics.checkNotNullExpressionValue(append7, "append(...)");
            Intrinsics.checkNotNullExpressionValue(append7.append('\n'), "append(...)");
            StringBuilder append8 = builder.append(this.tone);
            Intrinsics.checkNotNullExpressionValue(append8, "append(...)");
            Intrinsics.checkNotNullExpressionValue(append8.append('\n'), "append(...)");
        }
        if (!this.grammar.isEmpty()) {
            Intrinsics.checkNotNullExpressionValue(builder.append('\n'), "append(...)");
            StringBuilder append9 = builder.append("语法");
            Intrinsics.checkNotNullExpressionValue(append9, "append(...)");
            Intrinsics.checkNotNullExpressionValue(append9.append('\n'), "append(...)");
            Iterator it = this.grammar.iterator();
            while (it.hasNext()) {
                StringBuilder append10 = builder.append("• " + ((String) it.next()));
                Intrinsics.checkNotNullExpressionValue(append10, "append(...)");
                Intrinsics.checkNotNullExpressionValue(append10.append('\n'), "append(...)");
            }
        }
        String sb = builder.toString();
        Intrinsics.checkNotNullExpressionValue(sb, "toString(...)");
        return StringsKt.trim((CharSequence) sb).toString();
    }

    private final void appendNotes(StringBuilder builder, String title, List<TermNote> notes) {
        if (notes.isEmpty()) {
            return;
        }
        Intrinsics.checkNotNullExpressionValue(builder.append('\n'), "append(...)");
        StringBuilder append = builder.append(title);
        Intrinsics.checkNotNullExpressionValue(append, "append(...)");
        Intrinsics.checkNotNullExpressionValue(append.append('\n'), "append(...)");
        for (TermNote termNote : notes) {
            Iterable listOf = CollectionsKt.listOf((Object[]) new String[]{termNote.getReading(), termNote.getMeaning(), termNote.getNote()});
            Collection arrayList = new ArrayList();
            for (Object obj : listOf) {
                if (!StringsKt.isBlank((String) obj)) {
                    arrayList.add(obj);
                }
            }
            List list = (List) arrayList;
            builder.append("• ").append(termNote.getSurface());
            if (!list.isEmpty()) {
                builder.append("：").append(CollectionsKt.joinToString$default(list, "；", null, null, 0, null, null, 62, null));
            }
            Intrinsics.checkNotNullExpressionValue(builder.append('\n'), "append(...)");
        }
    }
}
