package com.hartwig.hmftools.linx.chaining;

import java.util.List;
import static java.lang.Math.pow;

public enum ChainingRule
{
    ASSEMBLY,
    AP_SUPPORT,
    ONLY,
    FOLDBACK_SPLIT,
    COMP_DUP_SPLIT,
    FOLDBACK,
    PLOIDY_MATCH,
    PLOIDY_OVERLAP,
    ADJACENT,
    ADJACENT_MATCH,
    PLOIDY_MAX,
    NEAREST;

    public static int ruleToPriority(ChainingRule rule)
    {
        switch(rule)
        {
            case AP_SUPPORT: return 9;
            case ONLY: return 8;
            case FOLDBACK_SPLIT: return 7;
            case COMP_DUP_SPLIT: return 7;
            case FOLDBACK: return 6;
            case PLOIDY_MATCH: return 5;
            case PLOIDY_OVERLAP: return 4;
            case ADJACENT: return 3;
            case ADJACENT_MATCH: return 3;
            case PLOIDY_MAX: return 2;
            case NEAREST: return 1;
            default: return 0;
        }
    }

    public static int calcRulePriority(final List<ChainingRule> rules)
    {
        return rules.stream().mapToInt(x -> (int)pow(2, ruleToPriority(x))).sum();
    }
}
