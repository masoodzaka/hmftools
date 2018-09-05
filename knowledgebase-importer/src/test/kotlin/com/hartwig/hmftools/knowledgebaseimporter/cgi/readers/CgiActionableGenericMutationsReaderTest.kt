package com.hartwig.hmftools.knowledgebaseimporter.cgi.readers

import com.hartwig.hmftools.knowledgebaseimporter.cgi.input.CgiActionableInput
import com.hartwig.hmftools.knowledgebaseimporter.knowledgebases.CodonMutations
import com.hartwig.hmftools.knowledgebaseimporter.knowledgebases.CodonRangeMutations
import com.hartwig.hmftools.knowledgebaseimporter.knowledgebases.GeneMutations
import com.hartwig.hmftools.knowledgebaseimporter.knowledgebases.SomaticEvent
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class CgiActionableGenericMutationsReaderTest : StringSpec() {

    companion object {
        private val actionableInput = CgiActionableInput("ESR1", null, "ESR1:.,E380Q,.537.,.538.,L536.,P535H,500-600", "MUT", "", "",
                                                         "", "", "", "", "", "")
    }

    init {
        "can read multiple codon mutations" {
            CgiActionableGenericMutationsReader.read(actionableInput) shouldBe listOf(
                    GeneMutations("ESR1", null),
                    CodonMutations("ESR1", null, 537),
                    CodonMutations("ESR1", null, 538),
                    CodonMutations("ESR1", null, 536),
                    CodonRangeMutations("ESR1", null, 500, 600))
        }

        "does not read from FUS input" {
            CgiActionableCodonMutationsReader.read(actionableInput.copy(`Alteration type` = "FUS")) shouldBe emptyList<SomaticEvent>()
        }
        "does not read from CNA input" {
            CgiActionableCodonMutationsReader.read(actionableInput.copy(`Alteration type` = "CNA")) shouldBe emptyList<SomaticEvent>()
        }
        "does not read from EXPR input" {
            CgiActionableCodonMutationsReader.read(actionableInput.copy(`Alteration type` = "EXPR")) shouldBe emptyList<SomaticEvent>()
        }
        "does not read from BIA input" {
            CgiActionableCodonMutationsReader.read(actionableInput.copy(`Alteration type` = "BIA")) shouldBe emptyList<SomaticEvent>()
        }
    }
}