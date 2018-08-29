/*******************************************************************************
 * ''' * QBiC User DB Tools enables users to add people and affiliations to our mysql user database.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.Item;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.shared.ui.combobox.FilteringMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Upload;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.FinishedListener;
import com.vaadin.ui.themes.ValoTheme;

import life.qbic.datamodel.persons.Person;
import life.qbic.portal.Styles;
import life.qbic.portal.Styles.NotificationType;
import life.qbic.userdb.helpers.PersonBatchReader;
import life.qbic.userdb.helpers.Uploader;

public class PersonBatchUpload extends VerticalLayout {

  private final Uploader uploader = new Uploader();

  private static final Logger logger = LogManager.getLogger(PersonBatchUpload.class);
  private Table table;
  private Button register;
  private Upload upload;
  private List<String> roleEnums;
  private Set<String> titleEnums;
  private Map<String, Integer> affiliationMap;

  private Map<Integer, Person> rowIDToPerson;

  public PersonBatchUpload(List<String> titleEnums, List<String> affiliationRoles,
      Map<String, Integer> affiMap) {
    this.roleEnums = affiliationRoles;
    this.titleEnums = new HashSet<String>(titleEnums);
    this.affiliationMap = affiMap;

    setMargin(true);
    setSpacing(true);

    // file upload component
    upload = new Upload("Laden Sie hier Ihre Datei hoch", uploader); //Upload your file here
    HorizontalLayout box = new HorizontalLayout();
    box.addComponent(upload);
    box.addComponent(Styles.getPopupViewContaining(getHelpComponent()));
    addComponent(box);

    // init person Table
    table = new Table("Personen"); //People
    table.setPageLength(1);
    table.setStyleName(ValoTheme.TABLE_SMALL);
    table.addContainerProperty("Titel", ComboBox.class, null);
    table.setColumnWidth("Titel", 90); //Title
    table.addContainerProperty("Vorname", String.class, null);
    table.addContainerProperty("Nachname", String.class, null);
    table.addContainerProperty("E-Mail", String.class, null);
    table.addContainerProperty("Telefon", String.class, null);
    table.addContainerProperty("Zugehörigkeit", ComboBox.class, null);
    table.addContainerProperty("Rolle", ComboBox.class, null);
    // next table
    addComponent(table);
    table.setVisible(false);

    // sample registration button
    register = new Button("Personen registrieren"); //Register People
    register.setVisible(false);
    addComponent(register);

    upload.setButtonCaption("Hochladen"); //Upload
    // Listen for events regarding the success of upload.
    upload.addFailedListener(uploader);
    upload.addSucceededListener(uploader);
    FinishedListener uploadFinListener = new FinishedListener() {
      /**
       * 
       */
      private static final long serialVersionUID = -8413963075202260180L;

      public void uploadFinished(FinishedEvent event) {
        String uploadError = uploader.getError();
        File file = uploader.getFile();
        if (file.getPath().endsWith("up_")) {
          String msg = "Keine Datein ausgewählt"; //No file selected.
          logger.warn(msg);
          Styles.notification("Das Lesen der Datei ist fehlgeschlagen.", msg, NotificationType.ERROR); //Failed to read file
          if (!file.delete())
            logger.error("uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
        } else {
          if (uploadError == null || uploadError.isEmpty()) {
            String msg = "Das Hochladen war erfolgreich!"; //Upload successful!
            logger.info(msg);
            try {
              setRegEnabled(false);
              PersonBatchReader parser = new PersonBatchReader();
              if (parser.readPeopleFile(file)) {
                fillTable(parser.getPeople());
              } else {
                String error = parser.getError();
                Styles.notification("Das Lesen der Datei ist fehlgeschlagen.", error, NotificationType.ERROR); //Failed to read file.
                if (!file.delete())
                  logger.error(
                      "uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
              }
            } catch (IOException e) {
              e.printStackTrace();
            }
          } else {
            Styles.notification("Das Hochladen ist fehlgeschlagen.", uploadError, NotificationType.ERROR);//Failed to upload file.
            if (!file.delete())
              logger
                  .error("uploaded tmp file " + file.getAbsolutePath() + " could not be deleted!");
          }
        }
      }
    };
    upload.addFinishedListener(uploadFinListener);
  }

  private Component getHelpComponent() {
    VerticalLayout v = new VerticalLayout();
    v.setSpacing(true);
    Label l = new Label(
        "Laden Sie eine TAB-separierte Datei hoch, welche Informationen über mehrere Personen enthält."); //Upload a tab-separated values file containing information about multiple people.
    l.setWidth("300px");
    v.addComponent(l);
    Button button = new Button("Beispiel herunterladen"); //Download Example
    v.addComponent(button);

    final File example =
        new File(getClass().getClassLoader().getResource("examples/people.tsv").getFile());
    FileDownloader tsvDL = new FileDownloader(new FileResource(example));
    tsvDL.extend(button);

    return v;
  }

  protected void fillTable(List<Person> people) {
    table.removeAllItems();
    rowIDToPerson = new HashMap<Integer, Person>();

    table.setPageLength(people.size());
    for (int i = 0; i < people.size(); i++) {
      int itemId = i;
      List<Object> row = new ArrayList<Object>();
      Person p = people.get(i);
      rowIDToPerson.put(i, p);
      // row.add(p.getUsername());

      ComboBox titleSelect = new ComboBox();
      titleSelect.setStyleName(Styles.boxTheme);
      titleSelect.setWidth("80px");
      titleSelect.addItems(titleEnums);
      titleSelect.setNullSelectionAllowed(false);
      titleSelect.setImmediate(true);
      if (titleEnums.contains(p.getTitle()))
        titleSelect.setValue(p.getTitle());
      row.add(titleSelect);

      row.add(p.getFirstName());
      row.add(p.getLastName());
      row.add(p.getEmail());
      row.add(p.getPhone());

      ComboBox affiliationSelect = new ComboBox();
      affiliationSelect.setStyleName(Styles.boxTheme);
      affiliationSelect.addItems(affiliationMap.keySet());
      affiliationSelect.setNullSelectionAllowed(false);
      affiliationSelect.setImmediate(true);
      affiliationSelect.setFilteringMode(FilteringMode.CONTAINS);

      row.add(affiliationSelect);

      ComboBox roleSelect = new ComboBox();
      roleSelect.setStyleName(Styles.boxTheme);
      roleSelect.addItems(roleEnums);
      roleSelect.setNullSelectionAllowed(false);
      roleSelect.setImmediate(true);
      row.add(roleSelect);

      table.addItem(row.toArray(new Object[row.size()]), itemId);
    }
    table.setVisible(true);
    setRegEnabled(true);
    register.setVisible(true);
  }

  public Button getRegisterButton() {
    return this.register;
  }

  public void setRegEnabled(boolean b) {
    register.setEnabled(b);
  }

  public List<Person> getPeople() {
    List<Person> people = new ArrayList<Person>();
    for (Object row : table.getItemIds()) {
      int id = (int) row;
      Item item = table.getItem(id);
      String title = (String) parseBoxCell(item, "Titel");
      String affiliation = (String) parseBoxCell(item, "Zugehörigkeit");
      String role = (String) parseBoxCell(item, "Rolle");
      Person p = rowIDToPerson.get(id);
      p.setTitle(title);
      p.addAffiliationInfo(affiliationMap.get(affiliation), affiliation, role);
      people.add(p);
    }
    return people;
  }

  public boolean isValid() {
    for (Object row : table.getItemIds()) {
      int id = (int) row;
      Item item = table.getItem(id);
      if (parseBoxCell(item, "Zugehörigkeit") == null) {
        Styles.notification("Fehlende Zugehörigkeit", "Bitte wählen Sie eine Zugehörigkeit für jede Person aus.",//"Missing Affiliation", "Please select an affiliation for each person."
            NotificationType.DEFAULT);
        return false;
      }
      if (parseBoxCell(item, "Rolle") == null) {
        Styles.notification("Fehlende Rolle", "Bitte wählen Sie eine Rolle für jede Person aus.",//"Missing Role", "Please select an affiliation role for each person.
            NotificationType.DEFAULT);
        return false;
      }
      if (parseBoxCell(item, "Titel") == null) {
        Styles.notification("Titel wird benötigt", //"Title needed",
            "Hochgeladener Titel ist unbekannt. Bitte benutzen Sie einen aus der zur Verfügung stehenden Auswahl.",
        		//"Uploaded Title is unknown. Please use one of the provided options.",
            NotificationType.DEFAULT);
        return false;
      }
    }
    return true;
  }

  private Object parseBoxCell(Item item, Object propertyId) {
    ComboBox c = (ComboBox) item.getItemProperty(propertyId).getValue();
    return c.getValue();
  }

}
