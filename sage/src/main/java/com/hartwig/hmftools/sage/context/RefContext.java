package com.hartwig.hmftools.sage.context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import com.google.common.primitives.Longs;
import com.hartwig.hmftools.common.genome.position.GenomePosition;
import com.hartwig.hmftools.sage.read.ReadContext;

import org.jetbrains.annotations.NotNull;

public class RefContext implements GenomePosition {

    private final String sample;
    private final String chromosome;
    private final long position;
    private final Map<String, AltContext> alts;

    private int rawDepth;
    private int rawRefSupport;
    private int rawRefSupportBaseQuality;

    public RefContext(final String sample, final String chromosome, final long position) {
        this.sample = sample;
        this.chromosome = chromosome;
        this.position = position;
        this.alts = new HashMap<>();
    }

    public RefContext(final RefContext left, final RefContext right) {
        this.sample = left.sample;
        this.chromosome = left.chromosome;
        this.position = left.position;
        this.alts = new HashMap<>();
        this.rawRefSupport = Math.min(left.rawRefSupport, right.rawRefSupport);
        this.rawDepth = Math.min(left.rawDepth, right.rawDepth);
    }

    public boolean isAltsEmpty() {
        return alts.isEmpty();
    }

    @NotNull
    public Collection<AltContext> alts() {
        return alts.values();
    }

    public void refRead(int baseQuality) {
        this.rawRefSupport++;
        this.rawDepth++;
        this.rawRefSupportBaseQuality += baseQuality;
    }

    @NotNull
    public AltContext altContext(@NotNull final String ref, @NotNull final String alt) {
        final String refAltKey = ref + "|" + alt;
        return alts.computeIfAbsent(refAltKey, key -> new AltContext(RefContext.this, ref, alt));
    }

    public void altRead(@NotNull final String ref, @NotNull final String alt, int baseQuality) {
        this.rawDepth++;
        final AltContext altContext = altContext(ref, alt);
        altContext.incrementAltRead(baseQuality);
    }


    public void altRead(@NotNull final String ref, @NotNull final String alt, int baseQuality, @NotNull final ReadContext interimReadContext) {
        this.rawDepth++;
        final AltContext altContext = altContext(ref, alt);
        altContext.incrementAltRead(baseQuality);
        altContext.addReadContext(interimReadContext);
    }

    @NotNull
    @Override
    public String chromosome() {
        return chromosome;
    }

    @Override
    public long position() {
        return position;
    }

    public int rawDepth() {
        return rawDepth;
    }

    public String sample() {
        return sample;
    }

    public int rawRefSupport() {
        return rawRefSupport;
    }

    public int rawRefSupportBaseQuality() {
        return rawRefSupportBaseQuality;
    }

    @Override
    public boolean equals(@Nullable Object another) {
        if (this == another) {
            return true;
        }
        return another instanceof RefContext && equalTo((RefContext) another);
    }

    private boolean equalTo(RefContext another) {
        return chromosome().equals(another.chromosome()) && position() == another.position();
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + chromosome().hashCode();
        h += (h << 5) + Longs.hashCode(position());
        return h;
    }

}
