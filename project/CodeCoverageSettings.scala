import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {

  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    ".*\\$anon.*",
    ".*Reverse.*",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    "uk.gov.hmrc.BuildInfo",
    "uk.gov.hmrc.disaaccount.AppInitialiser",
    "uk.gov.hmrc.disaaccount.config.AppConfig",
    "uk.gov.hmrc.disaaccount.Module",
    "uk.gov.hmrc.disaaccount.models.AgentPreferences",
    "uk.gov.hmrc.disaaccount.models.ClientDetails",
    "uk.gov.hmrc.disaaccount.models.Enumerable",
    "uk.gov.hmrc.disaaccount.models.ErrorResponse",
    "uk.gov.hmrc.disaaccount.models.StoredAgentPreferences",
    "uk.gov.hmrc.disaaccount.models.YesNoAnswer",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.BusinessVerification",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.CorrespondenceAddress",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.OrganisationEmail",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.UpdateRegistrationDetailsRequest",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.certificatesOfAuthority.CertificatesOfAuthority",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.certificatesOfAuthority.CertificatesOfAuthorityYesNo",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.isaProducts.IsaProducts",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.liaisonofficers.LiaisonOfficer",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.liaisonofficers.LiaisonOfficerCommunication",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.liaisonofficers.LiaisonOfficers",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.orgdetails.OrganisationDetails",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.RegistrationDetails",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.signatories.Signatories",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.signatories.Signatory",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.thirdparty.ThirdParty",
    "uk.gov.hmrc.disaaccount.models.registrationDetails.thirdparty.ThirdPartyOrganisations"
  )

  val settings: Seq[Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
