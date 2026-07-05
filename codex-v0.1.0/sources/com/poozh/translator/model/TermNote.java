package com.poozh.translator.model;

import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: AnalysisResult.kt */
@Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B-\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0003¢\u0006\u0004\b\u0007\u0010\bJ\t\u0010\u000e\u001a\u00020\u0003HÆ\u0003J\t\u0010\u000f\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0010\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0011\u001a\u00020\u0003HÆ\u0003J1\u0010\u0012\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\u0013\u001a\u00020\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u0016\u001a\u00020\u0017HÖ\u0001J\t\u0010\u0018\u001a\u00020\u0003HÖ\u0001R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\nR\u0011\u0010\u0005\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\f\u0010\nR\u0011\u0010\u0006\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\n¨\u0006\u0019"}, d2 = {"Lcom/poozh/translator/model/TermNote;", "", "surface", "", "reading", "meaning", "note", "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getSurface", "()Ljava/lang/String;", "getReading", "getMeaning", "getNote", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"}, k = 1, mv = {2, 0, 0}, xi = 48)
/* loaded from: classes7.dex */
public final /* data */ class TermNote {
    private final String meaning;
    private final String note;
    private final String reading;
    private final String surface;

    public static /* synthetic */ TermNote copy$default(TermNote termNote, String str, String str2, String str3, String str4, int i, Object obj) {
        if ((i & 1) != 0) {
            str = termNote.surface;
        }
        if ((i & 2) != 0) {
            str2 = termNote.reading;
        }
        if ((i & 4) != 0) {
            str3 = termNote.meaning;
        }
        if ((i & 8) != 0) {
            str4 = termNote.note;
        }
        return termNote.copy(str, str2, str3, str4);
    }

    /* renamed from: component1, reason: from getter */
    public final String getSurface() {
        return this.surface;
    }

    /* renamed from: component2, reason: from getter */
    public final String getReading() {
        return this.reading;
    }

    /* renamed from: component3, reason: from getter */
    public final String getMeaning() {
        return this.meaning;
    }

    /* renamed from: component4, reason: from getter */
    public final String getNote() {
        return this.note;
    }

    public final TermNote copy(String surface, String reading, String meaning, String note) {
        Intrinsics.checkNotNullParameter(surface, "surface");
        Intrinsics.checkNotNullParameter(reading, "reading");
        Intrinsics.checkNotNullParameter(meaning, "meaning");
        Intrinsics.checkNotNullParameter(note, "note");
        return new TermNote(surface, reading, meaning, note);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TermNote)) {
            return false;
        }
        TermNote termNote = (TermNote) other;
        return Intrinsics.areEqual(this.surface, termNote.surface) && Intrinsics.areEqual(this.reading, termNote.reading) && Intrinsics.areEqual(this.meaning, termNote.meaning) && Intrinsics.areEqual(this.note, termNote.note);
    }

    public int hashCode() {
        return (((((this.surface.hashCode() * 31) + this.reading.hashCode()) * 31) + this.meaning.hashCode()) * 31) + this.note.hashCode();
    }

    public String toString() {
        return "TermNote(surface=" + this.surface + ", reading=" + this.reading + ", meaning=" + this.meaning + ", note=" + this.note + ")";
    }

    public TermNote(String surface, String reading, String meaning, String note) {
        Intrinsics.checkNotNullParameter(surface, "surface");
        Intrinsics.checkNotNullParameter(reading, "reading");
        Intrinsics.checkNotNullParameter(meaning, "meaning");
        Intrinsics.checkNotNullParameter(note, "note");
        this.surface = surface;
        this.reading = reading;
        this.meaning = meaning;
        this.note = note;
    }

    public /* synthetic */ TermNote(String str, String str2, String str3, String str4, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this(str, (i & 2) != 0 ? "" : str2, (i & 4) != 0 ? "" : str3, (i & 8) != 0 ? "" : str4);
    }

    public final String getSurface() {
        return this.surface;
    }

    public final String getReading() {
        return this.reading;
    }

    public final String getMeaning() {
        return this.meaning;
    }

    public final String getNote() {
        return this.note;
    }
}
