/*******************************************************************************
 * QBiC User DB Tools enables users to add people and affiliations to our mysql user database.
 * Copyright (C) 2016 Andreas Friedrich
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package life.qbic.userdb.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.Person;
import life.qbic.portal.Styles;
import life.qbic.userdb.helpers.RegExHelper;

public class PersonInput extends HorizontalLayout {

  /**
   * 
   */
  private static final long serialVersionUID = 2657654653139639151L;
  private FormLayout left;

  private Button commit;
  private TextField userName;
  private ComboBox title;
  private TextField first;
  private TextField last;
  private TextField eMail;
  private TextField phone;
  private ComboBox affiliation;
  private CheckBox newAffiliation;
  private AffiliationInput affiInput;
  private ComboBox role;

  private Map<String, Integer> affiliationMap;

  public PersonInput(List<String> titles, Map<String, Integer> affiliations, List<String> roles,
      AffiliationInput affiInput) {
    left = new FormLayout();
    left.setMargin(true);

    affiliationMap = affiliations;
    this.affiInput = affiInput;
    this.affiInput.hideRegisterButton();
    this.affiInput.setVisible(false);

    userName = new TextField("Benutzername"); //Username
    // userName.setRequired(true);
    userName.addValidator(
        new RegexpValidator(RegExHelper.VALID_USERNAME_REGEX, "Bitte geben Sie einen gültigen Benutzernamen ein.")); //Please input a valid username.
    left.addComponent(Styles.questionize(userName,
    		"Universität Hohenheim Benutzername oder der von der CFH zur Verfügung gestellte Benutzername. Falls das Feld leer gelassen wird, wird ein Dummy Name gewählt, "
    	    		+ "welcher nicht zum Einloggen benutzt werden kann bis ein richtiger Benutzername hinzugefügt wurde. Personen Informationen können in diesem Fall trotzdem zu Projekten und Experimenten hinzugefügt werden.",
    	    		"Benutzername"));

    title = new ComboBox("Titel", titles);
    title.setRequired(true);
    title.setStyleName(ValoTheme.COMBOBOX_SMALL);
    title.setNullSelectionAllowed(false);
    left.addComponent(title);

    first = new TextField("Vorname");
    first.setRequired(true);
    first.addValidator(new RegexpValidator(RegExHelper.VALID_NAME_REGEX, "Bitte tragen Sie einen gültigen Namen ein."));
    left.addComponent(first);

    last = new TextField("Nachname");
    last.setRequired(true);
    last.addValidator(new RegexpValidator(RegExHelper.VALID_NAME_REGEX, "Bitte tragen Sie einen gültigen Namen ein."));
    left.addComponent(last);

    eMail = new TextField("E-Mail");
    eMail.setRequired(true);
    eMail.addValidator(new RegexpValidator(RegExHelper.VALID_EMAIL_ADDRESS_REGEX,
    		"Bitte geben Sie eine gültige E-Mail Adresse ein."));
    left.addComponent(eMail);

    phone = new TextField("Telefon");
    left.addComponent(phone);

    ArrayList<String> sortedAffiliations = new ArrayList<String>(affiliations.keySet());
    
    Collections.sort(sortedAffiliations);
    affiliation = new ComboBox("Zugehörigkeit", sortedAffiliations);
    // affiliation.setNullSelectionAllowed(false);
    affiliation.setRequired(true);
    affiliation.setFilteringMode(FilteringMode.CONTAINS);
    affiliation.setStyleName(ValoTheme.COMBOBOX_SMALL);
    left.addComponent(Styles.questionize(affiliation,
    		"Arbeitsgruppe oder Organisation in welche die Person zugehört. Falls noch keine im System vorhanden ist, muss zuerst eine \"Neue Zugehörigkeit\" erstellt werden. "
    	    		+"Zusätzliche Zugehörigkeiten und Rollen können im Reiter \"Zusätzliche Personen-Zugehörigkeiten\" gesetzt werden.",
        "Zugehörigkeit"));

    newAffiliation = new CheckBox("Neue Zugehörigkeit");
    left.addComponent(newAffiliation);
    newAffiliation.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        enableAffiliationInput(newAffiliation.getValue());
        affiliation.select(affiliation.getNullSelectionItemId());
        affiliation.setEnabled(!newAffiliation.getValue());
      }
    });

    role = new ComboBox("Rolle", roles); //Role
    role.setRequired(true);
    role.setStyleName(ValoTheme.COMBOBOX_SMALL);
    role.setNullSelectionAllowed(false);
    left.addComponent(role);

    commit = new Button("Benutzer registrieren"); //Save New User
    left.addComponent(commit);

    addComponent(left);
    addComponent(affiInput);
  }

  protected void enableAffiliationInput(Boolean enable) {
    if (enable)
      affiInput.reset();
    affiInput.setVisible(enable);
  }

  public boolean isValid() {
    boolean affiliationValid = affiInput.isValid() || affiliation.isValid();
    return userName.isValid() && title.isValid() && first.isValid() && last.isValid()
        && title.isValid() && eMail.isValid() && phone.isValid() && affiliationValid
        && role.isValid();
  }

  public Button getCommitButton() {
    return commit;
  }

  public Person getPerson() {
    String ttl = null;
    if (title.getValue() != null)
      ttl = title.getValue().toString();
    String affRole = null;
    if (role.getValue() != null)
      affRole = role.getValue().toString();
    String affi = (String) affiliation.getValue();
    int affiID = -1;
    if (affiliationMap.containsKey(affi))
      affiID = affiliationMap.get(affi);
    return new Person(userName.getValue(), ttl, first.getValue(), last.getValue(), eMail.getValue(),
        phone.getValue(), affiID, affi, affRole);
  }

  public boolean hasNewAffiliation() {
    return newAffiliation.getValue();
  }

  public Affiliation getNewAffiliation() {
    return affiInput.getAffiliation();
  }
}
