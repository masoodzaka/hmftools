package com.hartwig.hmftools.idgenerator

import com.hartwig.hmftools.idgenerator.anonymizedIds.HmfPatientId
import com.hartwig.hmftools.idgenerator.ids.PatientId
import com.hartwig.hmftools.idgenerator.ids.SampleId
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

private const val PASSWORD1 = "password"
private const val PASSWORD2 = "password_2"

class PatientAnonymizerTest : StringSpec() {
    private val anonymizer = PatientAnonymizer(PASSWORD1)
    private val generator1 = IdGenerator(PASSWORD1)
    private val generator2 = IdGenerator(PASSWORD2)

    private val patient1 = PatientId("CPCT01990001")!!
    private val patient2 = PatientId("CPCT01990002")!!
    private val patient3 = PatientId("CPCT01990003")!!
    private val sample1A = SampleId("${patient1.id}T")!!
    private val sample1B = SampleId("${patient1.id}TII")!!
    private val sample2A = SampleId("${patient2.id}T")!!
    private val sample3A = SampleId("${patient3.id}T")!!

    init {
        "generates patient id" {
            val samplesInput = SamplesInput(listOf(sample1A))
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())
            output.size shouldBe 1
            output[sample1A.patientId]!!.id shouldBe 1
        }

        "can add new patients to list" {
            val samplesInput = SamplesInput(listOf(sample1A))
            val updatedSamplesInput = SamplesInput(listOf(sample1A, sample2A))
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())

            val updatedOutput = anonymizer.anonymize(PASSWORD1, updatedSamplesInput, output)
            output.size shouldBe 1
            updatedOutput.size shouldBe 2
            updatedOutput[patient1]!!.id shouldBe output[patient1]!!.id
            updatedOutput[patient2]!!.id shouldBe 2
        }

        "removing an already anonymized patient does not delete it from output" {
            val samplesInput = SamplesInput(listOf(sample1A, sample2A))
            val updatedSamplesInput = SamplesInput(listOf(sample1A))
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())
            output.size shouldBe 2

            val updatedOutput = anonymizer.anonymize(PASSWORD1, updatedSamplesInput, output)
            updatedOutput.size shouldBe 2
            updatedOutput[patient1]!!.id shouldBe output[patient1]!!.id
            updatedOutput[patient2]!!.id shouldBe 2
        }

        "replacing an already anonymized patient adds one entry to output" {
            val samplesInput = SamplesInput(listOf(sample1A, sample2A))
            val updatedSamplesInput = SamplesInput(listOf(sample1A, sample3A))
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())
            output.size shouldBe 2

            val updatedOutput = anonymizer.anonymize(PASSWORD1, updatedSamplesInput, output)
            updatedOutput.size shouldBe 3
            updatedOutput[patient1]!!.id shouldBe output[patient1]!!.id
            updatedOutput[patient2]!!.id shouldBe 2
            updatedOutput[patient3]!!.id shouldBe 3
        }

        "correctly replaces an already anonymized patient while changing password" {
            val samplesInput = SamplesInput(listOf(sample1A, sample2A))
            val updatedSamplesInput = SamplesInput(listOf(sample1A, sample3A))
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())
            output.size shouldBe 2

            val updatedOutput = anonymizer.anonymize(PASSWORD2, updatedSamplesInput, output)
            updatedOutput.size shouldBe 3
            updatedOutput[patient1]!!.id shouldBe output[patient1]!!.id
            updatedOutput[patient2]?.id shouldBe null
            updatedOutput[patient3]!!.id shouldBe 3
            collisions(updatedOutput, output) shouldBe 1
        }

        "adding new sample for same patient does not add patients to output" {
            val samplesInput = SamplesInput(listOf(sample1A))
            val updatedSamplesInput = SamplesInput(listOf(sample1A, sample1B))
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())

            val updatedOutput = anonymizer.anonymize(PASSWORD1, updatedSamplesInput, output)
            updatedOutput.size shouldBe 1
            updatedOutput[patient1]!!.id shouldBe 1
        }

        "updates patient hash on password change" {
            val samplesInput = SamplesInput(listOf(sample1A))
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())

            val updatedOutput = anonymizer.anonymize(PASSWORD2, samplesInput, output)
            updatedOutput.size shouldBe output.size
            updatedOutput[patient1]!!.id shouldBe output[patient1]!!.id
            collisions(updatedOutput, output) shouldBe 0
        }

        "patients mapped from start generate single id" {
            val patientMap = mapOf(patient1 to patient2)
            val samplesInput = SamplesInput(listOf(sample1A, sample2A), patientMap)

            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())
            output.size shouldBe 1
            output[patient1]!!.id shouldBe 1
            output[patient2]!!.id shouldBe 1
            output[patient1]!!.hash shouldBe generator1.hash(patient2.id)
            output.anonymizedPatientMap() shouldBe emptyMap<HmfPatientId, HmfPatientId>()
        }

        "adding new sample for patients mapped from start keeps single id" {
            val patientMap = mapOf(patient1 to patient2)
            val samplesInput = SamplesInput(listOf(sample1A, sample2A), patientMap)
            val updatedSamplesInput = SamplesInput(listOf(sample1A, sample2A, sample1B), patientMap)
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())

            val updatedOutput = anonymizer.anonymize(PASSWORD1, updatedSamplesInput, output)
            updatedOutput.size shouldBe 1
            updatedOutput[patient1]!!.id shouldBe 1
            updatedOutput[patient2]!!.id shouldBe 1
            updatedOutput[patient1]!!.hash shouldBe generator1.hash(patient2.id)
            updatedOutput.anonymizedPatientMap() shouldBe emptyMap<HmfPatientId, HmfPatientId>()
        }

        "patients mapped from start generate single id after password change" {
            val patientMap = mapOf(patient1 to patient2)
            val samplesInput = SamplesInput(listOf(sample1A, sample2A), patientMap)
            val output = anonymizer.anonymize(PASSWORD1, samplesInput, emptyList())

            val updatedOutput = anonymizer.anonymize(PASSWORD2, samplesInput, output)
            updatedOutput.size shouldBe 1
            updatedOutput[patient1]!!.id shouldBe 1
            updatedOutput[patient2]!!.id shouldBe 1
            updatedOutput[patient1]!!.hash shouldBe generator2.hash(patient2.id)
            collisions(updatedOutput, output) shouldBe 0
            updatedOutput.anonymizedPatientMap() shouldBe emptyMap<HmfPatientId, HmfPatientId>()
        }

        "anonymized patients mapped later do not get deleted from output" {
            val patientMap = mapOf(patient1 to patient2)
            val inputWithoutMapping = SamplesInput(listOf(sample1A, sample2A), emptyMap())
            val inputWithMapping = SamplesInput(listOf(sample1A, sample2A), patientMap)

            val output = anonymizer.anonymize(PASSWORD1, inputWithoutMapping, emptyList())
            output.toSet().size shouldBe 2
            output[patient1]!!.id shouldBe 1
            output[patient2]!!.id shouldBe 2

            val updatedOutput = anonymizer.anonymize(PASSWORD1, inputWithMapping, output)
            updatedOutput.toSet() shouldBe output.toSet()
            updatedOutput[patient1]!!.id shouldBe 2
            updatedOutput[patient2]!!.id shouldBe 2
            updatedOutput.anonymizedPatientMap().size shouldBe 1
            updatedOutput.anonymizedPatientMap().map { it.key.id to it.value.id }.toMap() shouldBe mapOf(1 to 2)
        }

        "updates patient hash on password change for patients mapped later" {
            val patientMap = mapOf(patient1 to patient2)
            val inputWithoutMapping = SamplesInput(listOf(sample1A, sample2A), emptyMap())
            val inputWithMapping = SamplesInput(listOf(sample1A, sample2A), patientMap)

            val output = anonymizer.anonymize(PASSWORD1, inputWithoutMapping, emptyList())
            output.toSet().size shouldBe 2
            output[patient1]!!.id shouldBe 1
            output[patient2]!!.id shouldBe 2

            val updatedOutput = anonymizer.anonymize(PASSWORD2, inputWithMapping, output)
            updatedOutput.map { it.id }.toSet() shouldBe output.map { it.id }.toSet()
            collisions(updatedOutput, output) shouldBe 0
            updatedOutput[patient1]!!.id shouldBe 2
            updatedOutput[patient2]!!.id shouldBe 2
            updatedOutput.anonymizedPatientMap().size shouldBe 1
            updatedOutput.anonymizedPatientMap().map { it.key.id to it.value.id }.toMap() shouldBe mapOf(1 to 2)
        }
    }

    private fun collisions(patients: Collection<HmfPatientId>, otherPatients: Collection<HmfPatientId>): Int {
        val patientHashes = patients.map { it.hash }.toSet()
        val otherHashes = otherPatients.map { it.hash }.toSet()
        return patientHashes.count { otherHashes.contains(it) }
    }
}
