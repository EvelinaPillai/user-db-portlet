package life.qbic.userdb.views;

import java.util.List;
import java.util.Map;

import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.portal.Styles;
import life.qbic.userdb.helpers.RegExHelper;

public class EntryInput extends HorizontalLayout {

  private FormLayout personInput;
  private FormLayout groupInput;
  private Button commit;
  private TextField userName;
  private ComboBox title;
  private TextField first;
  private TextField last;
  private TextField eMail;
  private TextField phone;
  private ComboBox affiliation;
  private ComboBox role;
  
  public EntryInput(List<String> titles, Map<String, Integer> affiliations, List<String> roles) {
    setMargin(true);

    userName = new TextField("Benutzername"); //Username
    // userName.setRequired(true);
    userName.addValidator(
        new RegexpValidator(RegExHelper.VALID_USERNAME_REGEX, "Bitte geben Sie einen gültigen Benutzernamen ein.")); //Please input a valid username.
    addComponent(Styles.questionize(userName,
    		"Universität Hohenheim Benutzeranme oder der von der CFH zur Verfügung gestellte Benutzername. Falls das Feld leer gelassen wird, wird ein Pseudoname gewählt"
    		+ " welcher nicht zum Einloggen benutzt werden kannm bis ein richtiger Benutzername hinzugefügt wurde. Personen Informationen können in diesem Fall trotzdem zu Projekten und Experimenten hinzugefügt werden ",
    		"Benutzername"
//        "University Tübingen user name or user name provided by QBiC. If left empty a dummy user name is chosen "
//            + "which cannot be used to log in until a real name is added. Person information can still be added to "
//            + "projects or experiments in that case.",
//        "User Name"
    		));

    title = new ComboBox("Titel", titles); //Title
    title.setRequired(true);
    title.setStyleName(ValoTheme.COMBOBOX_SMALL);
    title.setNullSelectionAllowed(false);
    addComponent(title);

    first = new TextField("Vorname"); //First Name
    first.setRequired(true);
    first.addValidator(new RegexpValidator(RegExHelper.VALID_NAME_REGEX, "Bitte tragen Sie einen gültigen Namen ein.")); //Please input a valid name.
    addComponent(first);

    last = new TextField("Nachname");//Last Name
    last.setRequired(true);
    last.addValidator(new RegexpValidator(RegExHelper.VALID_NAME_REGEX, "Bitte tragen Sie einen gültigen Namen ein."));
    addComponent(last);

    eMail = new TextField("E-Mail");
    eMail.setRequired(true);
    eMail.addValidator(new RegexpValidator(RegExHelper.VALID_EMAIL_ADDRESS_REGEX,
    		"Bitte geben Sie eine gültige E-Mail Adresse ein."  ));//"Please input a valid e-mail address."
    addComponent(eMail);

    phone = new TextField("Telefon"); //Phone
    addComponent(phone);

    affiliation = new ComboBox("Zugehörigkeit", affiliations.keySet()); //Affiliation
    affiliation.setNullSelectionAllowed(false);
    affiliation.setRequired(true);
    affiliation.setFilteringMode(FilteringMode.CONTAINS);
    affiliation.setStyleName(ValoTheme.COMBOBOX_SMALL);
    addComponent(Styles.questionize(affiliation,
    		"Arbeitsgruppe oder Organisation in welche die Person zugehört. Falls noch keine im System vorhanden ist, muss zuerst eine \"Neue Zugehörigkeit\" erstellt werden. "
    		+"Zusätzliche Zugehörigkeiten und Rollen können im Reiter \"Zusätzliche Personen-Zugehörigkeiten\" gesetzt werden.",
//        "Work group or organization this person is part of. If it does not exist in the system "
//            + "a \"New Affiliation\" has to be created first. Additional Affiliations and roles can be set in the next Tab.",
//        "Affiliation"
    		"Zugehörigkeit"));

    role = new ComboBox("Rolle", roles);//Role
    role.setRequired(true);
    role.setStyleName(ValoTheme.COMBOBOX_SMALL);
    role.setNullSelectionAllowed(false);
    addComponent(role);

    commit = new Button("Benutzer registrieren");// Register User
    addComponent(commit);
  }
}
