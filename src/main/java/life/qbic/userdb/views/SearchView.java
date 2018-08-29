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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.persons.Affiliation;
import life.qbic.datamodel.persons.Person;
import life.qbic.datamodel.persons.PersonAffiliationConnectionInfo;
import life.qbic.portal.components.StandardTextField;

public class SearchView extends VerticalLayout {

  private StandardTextField inputPerson;
  private StandardTextField inputAffiliation;
  private Button searchPerson;
  private Table personsTable;
  private Table affiliationsOfPerson;

  private Button searchAffiliation;
  private Table affiliations;
  private Map<Integer, Person> listIDToPerson;

  public SearchView() {
    setSpacing(true);
    setMargin(true);

    setCaption("Nach Einträgen in der Datenbank suchen"); //Search for entries in the Database
        
   
    Label personLabel = new Label("Nach Personen suchen"); //Search for Person
    addComponent(personLabel);
    inputPerson = new StandardTextField();
    searchPerson = new Button("Suche"); //Search
    HorizontalLayout searchFields = new HorizontalLayout(inputPerson, searchPerson);
    searchFields.setSpacing(true);
    addComponent(searchFields);
    
    personsTable = new Table("Person"); //People
    personsTable.setPageLength(1);
    personsTable.setStyleName(ValoTheme.TABLE_SMALL);
    // persons.addContainerProperty("ID", Integer.class, null);
    // persons.addContainerProperty("User", String.class, null);
    personsTable.addContainerProperty("Titel", String.class, null);
    personsTable.addContainerProperty("Vorname", String.class, null);
    personsTable.addContainerProperty("Nachname", String.class, null);
    personsTable.addContainerProperty("E-Mail", String.class, null);
    personsTable.addContainerProperty("Telefon", String.class, null);
    // personsTable.addContainerProperty("(1st) Affiliation", String.class, null);
    // personsTable.addContainerProperty("Role", String.class, null);TODO maybe move this to the
    // next table
    addComponent(personsTable);
    personsTable.setSelectable(true);
    personsTable.addValueChangeListener(new ValueChangeListener() {

      @Override
      public void valueChange(ValueChangeEvent event) {
        affiliationsOfPerson.removeAllItems();
        affiliationsOfPerson.setVisible(false);
        Object id = personsTable.getValue();
        if (id != null) {
          Person p = listIDToPerson.get(id);
          affiliationsOfPerson.setCaption("Zugehörigkeiten von " + p.getFirstName() + " " + p.getLastName()); //Affiliations of 
          List<Affiliation> affiliations = p.getAffiliations();

          affiliationsOfPerson.setVisible(true);
          for (int i = 0; i < affiliations.size(); i++) {
            int itemId = i;
            List<Object> row = new ArrayList<Object>();
            Affiliation a = affiliations.get(i);
            row.add(a.getGroupName());
            row.add(a.getAcronym());
            row.add(a.getOrganization());
            row.add(a.getInstitute());
            row.add(a.getFaculty());
            row.add(a.getStreet());
            row.add(a.getZipCode());
            affiliationsOfPerson.addItem(row.toArray(new Object[row.size()]), itemId);
          }
          affiliationsOfPerson.setPageLength(affiliations.size());
        }
      }
    });

    affiliationsOfPerson = new Table();
    affiliationsOfPerson.setVisible(false);
    affiliationsOfPerson.setStyleName(ValoTheme.TABLE_SMALL);
    // affiliationsOfPerson.addContainerProperty("ID", Integer.class, null);
    affiliationsOfPerson.addContainerProperty("Gruppe", String.class, null);
    affiliationsOfPerson.addContainerProperty("Akronym", String.class, null);
    affiliationsOfPerson.addContainerProperty("Organisation", String.class, null);
    affiliationsOfPerson.addContainerProperty("Institut", String.class, null);
    affiliationsOfPerson.addContainerProperty("Fakultät", String.class, null);
    // affiliationsOfPerson.addContainerProperty("contactPerson", String.class, null);
    affiliationsOfPerson.addContainerProperty("Straße", String.class, null);
    affiliationsOfPerson.addContainerProperty("PLZ", String.class, null);
    addComponent(affiliationsOfPerson);

   
   
    Label affiliationLabel = new Label("Nach Zugehörigkeiten suchen"); //Search for Affiliation
    addComponent(affiliationLabel);
    inputAffiliation = new StandardTextField();
    searchAffiliation = new Button("Suche"); //Search
    HorizontalLayout affiliationSearch = new HorizontalLayout(inputAffiliation,searchAffiliation);
    affiliationSearch.setSpacing(true);
    addComponent(affiliationSearch);

    affiliations = new Table("Zugehörigkeiten"); //Affiliations
    affiliations.setStyleName(ValoTheme.TABLE_SMALL);
    // affiliations.addContainerProperty("ID", Integer.class, null);
    affiliations.addContainerProperty("Gruppe", String.class, null);
    affiliations.addContainerProperty("Akronym", String.class, null);
    affiliations.addContainerProperty("Organisation", String.class, null);
    affiliations.addContainerProperty("Institut", String.class, null);
    affiliations.addContainerProperty("Fakultät", String.class, null);
    // affiliations.addContainerProperty("contactPerson", String.class, null);
    affiliations.addContainerProperty("Straße", String.class, null);
    affiliations.addContainerProperty("PLZ", String.class, null);
    affiliations.setPageLength(1);
    addComponent(affiliations);
  }

  public Button getSearchPersonButton() {
    return searchPerson;
  }

  public Button getSearchAffiliationButton() {
    return searchAffiliation;
  }

  public TextField getPersonSearchField() {
    return inputPerson;
  }

  public TextField getAffiliationSearchField() {
    return inputAffiliation;
  }

  public void setPersons(List<Person> foundPersons) {
    listIDToPerson = new HashMap<Integer, Person>();
    personsTable.removeAllItems();
    personsTable.setPageLength(foundPersons.size());
    for (int i = 0; i < foundPersons.size(); i++) {
      int itemId = i;
      List<Object> row = new ArrayList<Object>();
      Person p = foundPersons.get(i);
      listIDToPerson.put(i, p);
      // row.add(p.getUsername());
      row.add(p.getTitle());
      row.add(p.getFirstName());
      row.add(p.getLastName());
      row.add(p.getEmail());
      row.add(p.getPhone());
      // RoleAt a = p.getOneAffiliationWithRole();
      // row.add(a.getAffiliation());
      // row.add(a.getRole());
      personsTable.addItem(row.toArray(new Object[row.size()]), itemId);
    }
  }

  public void setAffiliations(List<Affiliation> foundAffiliations) {
    affiliations.removeAllItems();
    affiliations.setPageLength(foundAffiliations.size());
    for (int i = 0; i < foundAffiliations.size(); i++) {
      int itemId = i;
      List<Object> row = new ArrayList<Object>();
      Affiliation a = foundAffiliations.get(i);
      row.add(a.getGroupName());
      row.add(a.getAcronym());
      row.add(a.getOrganization());
      row.add(a.getInstitute());
      row.add(a.getFaculty());
      row.add(a.getStreet());
      row.add(a.getZipCode());
      affiliations.addItem(row.toArray(new Object[row.size()]), itemId);
    }
  }
}
