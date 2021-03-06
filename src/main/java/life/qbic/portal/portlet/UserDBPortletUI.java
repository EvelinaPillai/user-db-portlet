package life.qbic.portal.portlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.portlet.PortletContext;
import javax.portlet.PortletSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.liferay.portal.model.User;
import com.liferay.portal.model.UserGroup;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.ShortcutAction;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.WrappedPortletSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;

import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Experiment;
import ch.systemsx.cisd.openbis.generic.shared.api.v1.dto.Project;
import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.CollaboratorWithResponsibility;
import life.qbic.datamodel.persons.Person;
import life.qbic.datamodel.projects.ProjectInfo;
import life.qbic.openbis.openbisclient.IOpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClient;
import life.qbic.openbis.openbisclient.OpenBisClientMock;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.portal.utils.PortalUtils;
import life.qbic.userdb.Config;
import life.qbic.userdb.DBManager;
import life.qbic.userdb.config.ConfigurationManagerFactory;
import life.qbic.userdb.views.AffiliationInput;
import life.qbic.userdb.views.AffiliationVIPTab;
import life.qbic.userdb.views.MultiAffiliationTab;
import life.qbic.userdb.views.PersonBatchUpload;
import life.qbic.userdb.views.PersonInput;
import life.qbic.userdb.views.ProjectView;
import life.qbic.userdb.views.SearchView;

/**
 * Entry point for portlet user-db-portlet. This class derives from {@link QBiCPortletUI}, which is
 * found in the {@code portal-utils-lib} library.
 */
@Theme("mytheme")
@SuppressWarnings("serial")
@Widgetset("life.qbic.portlet.AppWidgetSet")
public class UserDBPortletUI extends QBiCPortletUI {

  private static final Logger logger = LogManager.getLogger(UserDBPortletUI.class);
  private DBManager dbControl;
  private Map<String, Integer> affiMap;
  private Map<String, Integer> personMap;

  private TabSheet options;

  private Config config;
  public static String tmpFolder;

  private IOpenBisClient openbis;
  private final boolean testMode = false;
  private final boolean development = false;

  @Override
  protected Layout getPortletContent(final VaadinRequest request) {
    final VerticalLayout layout = new VerticalLayout();
    layout.setMargin(true);
    setContent(layout);

    options = new TabSheet();

    this.config = readConfig();
    tmpFolder = config.getTmpFolder();
    // LDAPConfig ldapConfig = readLdapConfig();// TODO

    // establish connection to the OpenBIS API
    if (!development || !testMode) {
      try {
        this.openbis = new OpenBisClient(config.getOpenbisUser(), config.getOpenbisPass(),
            config.getOpenbisURL());
        this.openbis.login();
      } catch (Exception e) {
        // success = false;
        // logger.error(
        // "User \"" + userID + "\" could not connect to openBIS and has been informed of this.");
        // layout.addComponent(new Label(
        // "Data Management System could not be reached. Please try again later or contact us."));
      }
    }
    if (development && testMode) {
      logger.error("No connection to openBIS. Trying mock version for testing.");
      this.openbis = new OpenBisClientMock("", "", "");
      layout.addComponent(new Label(
          "openBIS could not be reached. Resuming with mock version. Some options might be non-functional. Reload to retry."));
    }

    dbControl = new DBManager(config);

    initTabs();

    layout.addComponent(options);
    return layout;
  }


  private void initTabs() {
    boolean admin = isAdmin();
    options.removeAllComponents();
    if (!admin && !development && !canUsePortlet()) {
      VerticalLayout rightsMissingTab = new VerticalLayout();
      rightsMissingTab.setCaption("User Database Input");
      Label info = new Label("Ihr Benutzerkonto besitzt nicht die erforderlichen administrativen Rechte, um neue Personen in unsere Datenbank hinzuzufügen.\n"
    		  +"Wenn Sie der Ansicht sind, dass Sie diese Rechte haben sollten dann kontaktieren Sie uns bitte.",
          //"Your account does not have the necessary rights to add new people to our database.\n"
              //+ "If you think you should be able to do so, please contact us.",
          ContentMode.PREFORMATTED);
      rightsMissingTab.addComponent(info);
      options.addTab(rightsMissingTab, "Information");
      options.setSelectedTab(rightsMissingTab);
      options.setEnabled(false);

    } else {

      affiMap = dbControl.getAffiliationMap();
      personMap = dbControl.getPersonMap();
      Set<String> instituteNames = dbControl.getInstituteNames();
      List<String> facultyEnums =
          dbControl.getPossibleEnumsForColumnsInTable("organizations", "faculty");
      List<String> affiliationRoles =
          dbControl.getPossibleEnumsForColumnsInTable("persons_organizations", "occupation");
      List<String> titleEnums = dbControl.getPossibleEnumsForColumnsInTable("persons", "title");

      PersonInput addUserTab = new PersonInput(titleEnums, affiMap, affiliationRoles,
          new AffiliationInput(instituteNames, facultyEnums, personMap));
      options.addTab(addUserTab, "Neue Person"); //"New Person"

      AffiliationInput addAffilTab = new AffiliationInput(instituteNames, facultyEnums, personMap);
      options.addTab(addAffilTab, "Neue Zugehörigkeit"); //"New Affiliation"


      SearchView searchView = new SearchView();
      options.addTab(searchView, "Einträge durchsuchen"); //"Search Entries"

      List<Affiliation> affiTable = dbControl.getAffiliationTable();
      Map<Integer, Pair<String, String>> affiPeople = new HashMap<Integer, Pair<String, String>>();
      for (Affiliation a : affiTable) {
        int id = a.getID();
        affiPeople.put(id,
            new ImmutablePair<String, String>(a.getContactPerson(), a.getHeadName()));
      }

      PersonBatchUpload batchTab = new PersonBatchUpload(titleEnums, affiliationRoles, affiMap);
      options.addTab(batchTab, "Personen-Tabelle hochladen"); //"Upload Person Table"

      AffiliationVIPTab vipTab = new AffiliationVIPTab(personMap, affiMap, affiPeople);
      options.addTab(vipTab, "Zugehörigkeit VIPs editieren"); //"Edit Affiliation VIPs"

      MultiAffiliationTab multiAffilTab =
          new MultiAffiliationTab(personMap, affiMap, affiliationRoles);
      options.addTab(multiAffilTab, "Zusätzliche Personen-Zugehörigkeiten"); //"Additional Person-Affiliations"
      if (!admin) {
        options.getTab(3).setEnabled(false);
        options.getTab(4).setEnabled(false);
      }

      String userID = "";
      if (PortalUtils.isLiferayPortlet()) {
        logger.info("DB Tools running on Liferay, fetching user ID.");
        userID = PortalUtils.getUser().getScreenName();
      } else {
        if (development) {
          logger.warn("Checks for local dev version successful. User is granted admin status.");
          userID = "admin";
        }
      }
      Map<String, ProjectInfo> userProjects = new HashMap<String, ProjectInfo>();

      List<Project> openbisProjects = new ArrayList<Project>();
      if (testMode) {
        openbisProjects = openbis.listProjects();
      } else {
        openbisProjects = openbis.getOpenbisInfoService()
            .listProjectsOnBehalfOfUser(openbis.getSessionToken(), userID);
      }
      Map<String, ProjectInfo> allProjects = dbControl.getProjectMap();
      for (Project p : openbisProjects) {
        String projectID = p.getIdentifier();
        String code = p.getCode();
        if (allProjects.get(projectID) == null)
          userProjects.put(projectID, new ProjectInfo(p.getSpaceCode(), code, "", -1));
        else
          userProjects.put(projectID, allProjects.get(projectID));
      }

      ProjectView projectView = new ProjectView(userProjects.values(), openbis, personMap);
      options.addTab(projectView, "Projekte"); //Projects
      options.getTab(5).setEnabled(!userProjects.isEmpty());

      initPortletToDBFunctionality(addAffilTab, addUserTab, batchTab, multiAffilTab, vipTab,
          searchView, projectView);
    }
  }

  private boolean canUsePortlet() {
    try {
      User user = PortalUtils.getUser();
      for (UserGroup grp : user.getUserGroups()) {
        String group = grp.getName();
        if (config.getUserGrps().contains(group)) {
          logger.info("User " + user.getScreenName() + " can use portlet because they are part of "
              + group);
          return true;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Could not fetch user groups. User won't be able to use portlet.");
    }
    return false;
  }

  private boolean isAdmin() {
    if (development)
      return true;
    else {
      try {
        User user = PortalUtils.getUser();
        for (UserGroup grp : user.getUserGroups()) {
          String group = grp.getName();
          if (config.getAdminGrps().contains(group)) {
            logger.info("User " + user.getScreenName()
                + " has full rights because they are part of " + group);
            return true;
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        logger.error("Could not fetch user groups. User won't be able to use portlet.");
      }
      return false;
    }
  }

  private void initPortletToDBFunctionality(final AffiliationInput addAffilTab,
      final PersonInput addUserTab, final PersonBatchUpload batchUpload,
      final MultiAffiliationTab multiAffilTab, final AffiliationVIPTab vipTab,
      final SearchView search, final ProjectView projects) {

    batchUpload.getRegisterButton().addClickListener(new Button.ClickListener() {
      /**
       * 
       */
      private static final long serialVersionUID = 1L;

      /**
       * 
       */

      @Override
      public void buttonClick(ClickEvent event) {
        if (batchUpload.isValid()) {
          List<String> registered = new ArrayList<String>();
          batchUpload.setRegEnabled(false);
          for (Person p : batchUpload.getPeople()) {
            if (dbControl.personExists(p)) {
              registered.add(p.getFirstName() + " " + p.getLastName());
            } else {
              if (!dbControl.addNewPerson(p)) {
                commitError("There has been an error while adding a new person.");
              }
            }
          }
          if (registered.isEmpty())
            successfulCommit();
          else {
            Styles.notification("Person ist bereits registriert", //"Person already registered",
                StringUtils.join(registered, ", ")
                    + " hat bereits einen Benutzernamen oder eine E-Mail in unserer Datenbank! Daher werden sie im Registrierungsprozess übersprungen.", 
                    //" had a username or email already registered in our database! They were skipped in the registration process.",
                NotificationType.DEFAULT);
          }
        }
      }
    });

    projects.getProjectTable().addValueChangeListener(new ValueChangeListener() {

      private Map<String, String> expTypeCodeTranslation = new HashMap<String, String>() {
        {
          put("Q_EXPERIMENTAL_DESIGN", "Patients/Sources");
          put("Q_SAMPLE_EXTRACTION", "Sample Extracts");
          put("Q_SAMPLE_PREPARATION", "Sample Preparations");
          put("Q_MS_MEASUREMENT", "Mass Spectrometry");
          put("Q_NGS_MEASUREMENT", "NGS Sequencing");
          put("Q_CFH_ELEMENT", "Element Analysis");
          put("Q_CFH_NMIN", "Nmin Analysis");
        };
      };

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object item = projects.getProjectTable().getValue();
        if (item != null) {
          String project = item.toString();
          // get collaborators associated to openbis experiments
          List<CollaboratorWithResponsibility> collaborators =
              dbControl.getCollaboratorsOfProject(project);
          // get openbis experiments and type
          Map<String, String> existingExps = new HashMap<String, String>();
          for (Experiment e : openbis.getExperimentsForProject2(project)) {
            String type = expTypeCodeTranslation.get(e.getExperimentTypeCode());
            String id = e.getIdentifier();
            if (type != null)
              existingExps.put(id, type);
          }
          // add types for experiments with existing collaborators
          for (CollaboratorWithResponsibility c : collaborators) {
            String identifier = c.getOpenbisIdentifier();
            c.setType(existingExps.get(identifier));
            existingExps.remove(identifier);
          }
          // add empty entries and type for applicable experiments without collaborators
          for (String expID : existingExps.keySet()) {
            String code = expID.split("/")[3];
            CollaboratorWithResponsibility c =
                new CollaboratorWithResponsibility(-1, "", expID, code, "Contact");
            c.setType(existingExps.get(expID));
            collaborators.add(c);
          }
          projects.setCollaboratorsOfProject(collaborators);
        }
      }
    });

    projects.getInfoCommitButton().addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        ProjectInfo info = projects.getEditedInfo();
        if (info != null) {
          String code = info.getProjectCode();
          int id = info.getProjectID();
          if (id < 1)
            id = dbControl.addProjectToDB("/" + info.getSpace() + "/" + code,
                info.getSecondaryName());
          else
            dbControl.addOrChangeSecondaryNameForProject(id, info.getSecondaryName());
          if (info.getInvestigator() == null || info.getInvestigator().isEmpty())
            dbControl.removePersonFromProject(id, "PI");
          else
            dbControl.addOrUpdatePersonToProject(id, personMap.get(info.getInvestigator()), "PI");
          if (info.getContact() == null || info.getContact().isEmpty())
            dbControl.removePersonFromProject(id, "Contact");
          else
            dbControl.addOrUpdatePersonToProject(id, personMap.get(info.getContact()), "Contact");
          if (info.getManager() == null || info.getManager().isEmpty())
            dbControl.removePersonFromProject(id, "Manager");
          else
            dbControl.addOrUpdatePersonToProject(id, personMap.get(info.getManager()), "Manager");
          projects.updateChangedInfo(info);
        }
      }
    });;
    projects.getPeopleCommitButton().addClickListener(new ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        List<CollaboratorWithResponsibility> links = projects.getNewResponsibilities();
        for (CollaboratorWithResponsibility c : links) {
          int experimentID = c.getExperimentID();
          if (experimentID < 1)
            experimentID = dbControl.addExperimentToDB(c.getOpenbisIdentifier());
          String name = c.getPerson();
          int personID = -1;
          if (personMap.get(name) != null)
            personID = personMap.get(name);
          if (personID < 1)
            dbControl.removePersonFromExperiment(experimentID);
          else
            dbControl.addOrUpdatePersonToExperiment(experimentID, personID, "Contact");
        }
      }
    });;

    search.getSearchAffiliationButton().addClickListener(new Button.ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        String affi = search.getAffiliationSearchField().getValue();
        if (affi != null && !affi.isEmpty()) {
          search.setAffiliations(dbControl.getAffiliationsContaining(affi));
        } else
          search.setAffiliations(new ArrayList<Affiliation>());
      }
    });
    
    search.getSearchPersonButton().addClickListener(new Button.ClickListener() {
      @Override
      public void buttonClick(ClickEvent event) {
        String person = search.getPersonSearchField().getValue();
        if (person != null && !person.isEmpty()) {
          search.setPersons(dbControl.getPersonsContaining(person));
        } else
          search.setPersons(new ArrayList<Person>());
      }
    });
    
    addAffilTab.getCommitButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (addAffilTab.isValid()) {
          if (dbControl.addNewAffiliation(addAffilTab.getAffiliation()) > -1)
            successfulCommit();
          else
            commitError("Ein Fehler ist aufgetreten."); //"There has been an error."
        } else
          inputError();
      }
    });

    vipTab.getSetHeadAndContactButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        int affi = vipTab.getSelectedAffiTabID();
        int contact = vipTab.getNewContactID();
        int head = vipTab.getNewHeadID();
        if (affi > 0) {
          if (head > 0)
            dbControl.setAffiliationVIP(affi, head, "head");
          if (contact > 0)
            dbControl.setAffiliationVIP(affi, contact, "main_contact");
          vipTab.updateVIPs();
          successfulCommit();
        }
      }
    });

    addAffilTab.getInstituteField().addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        Object val = addAffilTab.getInstituteField().getValue();
        if (val != null) {
          Affiliation orgInfo = dbControl.getOrganizationInfosFromInstitute(val.toString());
          if (orgInfo != null)
            addAffilTab.autoComplete(orgInfo);
        }
      }
    });

    addUserTab.getCommitButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (addUserTab.isValid()) {
          Person p = addUserTab.getPerson();
          if (addUserTab.hasNewAffiliation()) {
            int affiID = dbControl.addNewAffiliation(addUserTab.getNewAffiliation());
            if (affiID > -1)
              successfulCommit();
            else
              commitError("Ein Fehler ist während des Hinzufügens einer neuen Zugehörigkeit aufgetreten."); //There has been an error while adding the new affiliation.
            p.setAffiliationID(affiID);
          }
          if (dbControl.personExists(p)) {
            Styles.notification("Person ist bereits registriert", //"Person already registered"
                "Eine Person mit diesem Benutzernamen oder dieser E-Mail ist bereits in unserer Datenbank registriert!",
            		//"A person with the Username or E-Mail you selected is already registered in our database!",
                NotificationType.ERROR);
          } else {
            if (dbControl.addNewPerson(p))
              successfulCommit();
            else
              commitError("Ein Fehler ist während des Hinzufügens einer neuen Person aufgetreten."); //There has been an error while adding a new person.
          }
        } else
          inputError();
      }
    });

    multiAffilTab.getCommitButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        if (multiAffilTab.isValid()) {
          if (dbControl.addOrUpdatePersonAffiliationConnections(
              personMap.get(multiAffilTab.getPersonBox().getValue()),
              multiAffilTab.getChangedAndNewConnections()))
            successfulCommit();
          else
            commitError("Ein Fehler ist aufgetreten"); //There has been an error.
        } else
          inputError();
      }
    });

    multiAffilTab.getAddButton().addClickListener(new Button.ClickListener() {

      @Override
      public void buttonClick(ClickEvent event) {
        String personName = multiAffilTab.getPersonBox().getValue().toString();
        Person p = dbControl.getPerson(personMap.get(personName));

        String affiName = multiAffilTab.getOrganizationBox().getValue().toString();
        Person newP = new Person(p.getUsername(), p.getTitle(), p.getFirstName(), p.getLastName(),
            p.getEmail(), p.getPhone(), affiMap.get(affiName), affiName, "");
        multiAffilTab.addDataToTable(new ArrayList<Person>(Arrays.asList(newP)));
        multiAffilTab.getAddButton().setEnabled(false);
      }
    });

    ValueChangeListener multiAffiPersonListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (multiAffilTab.getPersonBox().getValue() != null) {
          String personName = multiAffilTab.getPersonBox().getValue().toString();
          multiAffilTab.reactToPersonSelection(
              dbControl.getPersonWithAffiliations(personMap.get(personName)));
          multiAffilTab.getAddButton().setEnabled(multiAffilTab.newAffiliationPossible());
        }
      }
    };
    multiAffilTab.getPersonBox().addValueChangeListener(multiAffiPersonListener);

    ValueChangeListener multiAffiListener = new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        if (multiAffilTab.getPersonBox().getValue() != null) {
          multiAffilTab.getAddButton().setEnabled(multiAffilTab.newAffiliationPossible());
        }
      }
    };
    multiAffilTab.getOrganizationBox().addValueChangeListener(multiAffiListener);
  }

  private void successfulCommit() {
    Styles.notification("Daten hinzugefügt","Die Daten wurden erfolgreich zur Datenbank hinzugefügt.", 
    		//"Data added", "Data has been successfully added to the database!
        NotificationType.SUCCESS);
    // wait a bit and reload tabs
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    initTabs();
  }

  private void inputError() {
    Styles.notification("Daten unvollständig", "Bitte füllen Sie alle benötigten Felder korrekt aus.",
//    		"Data Incomplete", "Please fill in all required fields correctly.",
        NotificationType.DEFAULT);
  }

  private void commitError(String reason) {
    Styles.notification("Ein Fehler ist aufgetreten", reason, NotificationType.ERROR); //There has been an error.
  }

  private Config readConfig() {
    life.qbic.userdb.config.ConfigurationManager c = ConfigurationManagerFactory.getInstance();

    return new Config(c.getMysqlHost(), c.getMysqlPort(), c.getMysqlDB(), c.getMysqlUser(),
        c.getMysqlPass(), c.getDBInputUserGrps(), c.getDBInputAdminGrps(), c.getDataSourceUrl(),
        c.getDataSourceUser(), c.getDataSourcePassword(), c.getTmpFolder());
  }

  private String getPortletContextName(VaadinRequest request) {
    WrappedPortletSession wrappedPortletSession =
        (WrappedPortletSession) request.getWrappedSession();
    PortletSession portletSession = wrappedPortletSession.getPortletSession();

    final PortletContext context = portletSession.getPortletContext();
    final String portletContextName = context.getPortletContextName();
    return portletContextName;
  }
}
