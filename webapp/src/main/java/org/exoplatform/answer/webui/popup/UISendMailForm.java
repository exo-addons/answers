/***************************************************************************
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 ***************************************************************************/
package org.exoplatform.answer.webui.popup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.InternetAddress;

import org.exoplatform.answer.webui.BaseUIFAQForm;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.UIQuestions;
import org.exoplatform.commons.utils.StringCommonUtils;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.QuestionLanguage;
import org.exoplatform.forum.common.EmailNotifyPlugin;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.services.mail.MailService;
import org.exoplatform.services.mail.Message;
import org.exoplatform.services.organization.User;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormRichtextInput;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.validator.MandatoryValidator;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class, 
    template = "app:/templates/answer/webui/popup/UISendMailForm.gtmpl", 
    events = {
        @EventConfig(listeners = UISendMailForm.SendActionListener.class), 
        @EventConfig(listeners = UISendMailForm.ToActionListener.class, phase=Phase.DECODE), 
        @EventConfig(listeners = UISendMailForm.CancelActionListener.class, phase=Phase.DECODE), 
        @EventConfig(listeners = UISendMailForm.ChangeLanguageActionListener.class, phase=Phase.DECODE) 
    }
)
public class UISendMailForm extends BaseUIFAQForm implements UIPopupComponent {

  private static final String            FILED_FROM_NAME         = "FromName";

  private static final String            FILED_FROM              = "From";

  private static final String            FILED_TO                = "To";

  private static final String            FILED_SUBJECT           = "Subject";

  private static final String            FILED_QUESTION_LANGUAGE = "Language";

  private static final String            FILED_MESSAGE           = "Message";

  final static public String             FIELD_FROM_INPUT        = "fromInput";

  final private static String            MIMETYPE_TEXTHTML       = "text/html".intern();

  private Map<String, String>            serverConfig_           = new HashMap<String, String>();

  private List<SelectItemOption<String>> listLanguageToReponse   = new ArrayList<SelectItemOption<String>>();

  private List<QuestionLanguage>         listQuestionLanguage    = new ArrayList<QuestionLanguage>();

  private String                         languageIsResponsed     = "";

  private String                         questionId              = "";

  protected String                       questionChanged_        = "";

  public List<User>                      toUsers                 = new ArrayList<User>();

  private int                            posOfResponse           = 0;

  private List<String>                   listAnotherEmail        = new ArrayList<String>();

  public UISendMailForm() throws Exception {
    listAnotherEmail = new ArrayList<String>();
    this.setActions(new String[] { "Send", "Cancel" });
  }

  public void activate() {
  }

  public void deActivate() {
  }

  public List<User> getToUsers() {
    return toUsers;
  }

  public void setToUsers(List<User> userList) {
    toUsers = userList;
  }

  protected List<SelectItemOption<String>> getListLanguageToSendFriend() {
    return listLanguageToReponse;
  }

  public void addPlugin(ComponentPlugin plugin) throws Exception {
    try {
      serverConfig_ = ((EmailNotifyPlugin) plugin).getServerConfiguration();
    } catch (Exception e) {
      log.error("Can not add Plugin Email Norify, exception: " + e.getMessage());
    }
  }

  public void setUpdateQuestion(String questionPath, String language) throws Exception {
    Question question = FAQUtils.getFAQService().getQuestionById(questionPath);
    if (language.length() <= 0)
      language = question.getLanguage();
    String email = "";
    String name = "" ;
    String userName = FAQUtils.getCurrentUser() ;
    if(!FAQUtils.isFieldEmpty(userName)){
      name = FAQUtils.getFullName(null) ;
      email = FAQUtils.getEmailUser(null) ;
    }
    languageIsResponsed = question.getLanguage();
    QuestionLanguage questionLanguage = new QuestionLanguage();
    questionLanguage.setId(question.getId());
    questionLanguage.setQuestion(question.getQuestion());
    questionLanguage.setDetail(question.getDetail());
    questionLanguage.setLanguage(question.getLanguage());
    questionLanguage.setAnswers(question.getAnswers());
    questionLanguage.setComments(question.getComments());

    listQuestionLanguage.add(questionLanguage);
    for (QuestionLanguage questionLanguage2 : getFAQService().getQuestionLanguages(questionPath)) {
      String quest2 = questionLanguage2.getDetail().replaceAll("\n", "<br>").replaceAll("'", "&#39;");
      questionLanguage2.setDetail(quest2);
      if (!isContainLanguageList(listQuestionLanguage, questionLanguage2.getLanguage()))
        listQuestionLanguage.add(questionLanguage2);
    }
    questionChanged_ = question.getQuestion();

    listLanguageToReponse.add(new SelectItemOption<String>(language, language));

    addChild(new UIFormStringInput(FILED_FROM_NAME, FILED_FROM_NAME, name).addValidator(MandatoryValidator.class));
    addChild(new UIFormStringInput(FILED_FROM, FILED_FROM, email).addValidator(MandatoryValidator.class));
    addChild(new UIFormStringInput(FILED_TO, FILED_TO, null).addValidator(MandatoryValidator.class));
    UIFormSelectBox questionLanguages = new UIFormSelectBox(FILED_QUESTION_LANGUAGE, FILED_QUESTION_LANGUAGE, listLanguageToReponse);
    questionLanguages.setSelectedValues(new String[] { language });
    questionLanguages.setOptions(listLanguageToReponse);
    questionLanguages.setOnChange("ChangeLanguage");
    questionLanguages.setHTMLAttribute("id", FILED_QUESTION_LANGUAGE);
    addChild(questionLanguages);
    // question

    String contenQuestion = "";
    String qsId = question.getId();
    StringBuffer stringBuffer = new StringBuffer();
    for (QuestionLanguage questionLangua : listQuestionLanguage) {
      if (questionLangua.getLanguage().equals(language)) {
        contenQuestion = questionLangua.getQuestion();
        Answer[] answers = questionLangua.getAnswers();
        stringBuffer.append(getLabel("change-content")).append(":<p><strong>").append(getLabel("Question")).append("</strong> ").append(contenQuestion).append("</p>");
        if (questionLangua.getDetail() != null && questionLangua.getDetail().trim().length() > 0)
          stringBuffer.append("<p><strong>").append(this.getLabel("Detail")).append("</strong> ").append(questionLangua.getDetail()).append("</p>");
        if (answers != null && answers.length > 0) {
          stringBuffer.append("<p>");
          for (Answer answer : answers) {
            stringBuffer.append("<br/><strong>").append(getLabel("Response")).append("</strong>").append("<br/>").append(answer.getResponses());
          }
          stringBuffer.append("</p>");
        }
        if (!language.equals(languageIsResponsed)) {
          qsId = qsId.concat(UIQuestions.OBJECT_LANGUAGE).concat(language);
        }
        stringBuffer.append(getLabel("Link").replaceFirst("<link>", FAQUtils.getQuestionURL(qsId, false)));
        break;
      }
    }

    addChild(new UIFormStringInput(FILED_SUBJECT, FILED_SUBJECT, contenQuestion.replaceAll("<br\\s*\\/?>", " "))
	  .addValidator(MandatoryValidator.class));
    UIFormRichtextInput filedMessage = new UIFormRichtextInput(FILED_MESSAGE, FILED_MESSAGE, "");
    filedMessage.setIgnoreParserHTML(true).setIsPasteAsPlainText(true)
                .setToolbar(UIFormRichtextInput.FAQ_TOOLBAR);
    filedMessage.setValue(StringCommonUtils.decodeSpecialCharToHTMLnumber(stringBuffer.toString()))
                .addValidator(MandatoryValidator.class);
    addChild(filedMessage);
  }

  private boolean isContainLanguageList(List<QuestionLanguage> questionlg, String language) throws Exception {
    for (QuestionLanguage questionLanguage : questionlg) {
      if (questionLanguage.getLanguage().equals(language))
        return true;
    }
    return false;
  }

  public void setFieldToValue(String value) {
    if (listAnotherEmail != null && listAnotherEmail.size() > 0) {
      for (String email : listAnotherEmail) {
        value = email + "," + value;
      }
    }
    getUIStringInput(FILED_TO).setValue(value);
  }

  public String getFieldToValue() {
    return getUIStringInput(FILED_TO).getValue();
  }

  public void setFieldCCValue(String value) {
    if (listAnotherEmail != null && listAnotherEmail.size() > 0) {
      for (String email : listAnotherEmail) {
        value = email + "," + value;
      }
    }
  }

  static public class SendActionListener extends BaseEventListener<UISendMailForm> {
    public void onEvent(Event<UISendMailForm> event, UISendMailForm sendMailForm, String objectId) throws Exception {
      String fromName = sendMailForm.getUIStringInput(FILED_FROM_NAME).getValue();
      String from = sendMailForm.getUIStringInput(FILED_FROM).getValue();
      String fullFrom = fromName + " (" + from + ") <" + sendMailForm.getServerConfig().get("account") + ">";
      String to = sendMailForm.getUIStringInput(FILED_TO).getValue();
      String subject = sendMailForm.getUIStringInput(FILED_SUBJECT).getValue();
      String body = sendMailForm.getUIFormRichtextInput(FILED_MESSAGE).getValue();
      if (to != null && to.indexOf(";") > -1) {
        to = to.replace(';', ',');
      }
      if (FAQUtils.isFieldEmpty(fromName)) {
        warning("UISendMailForm.msg.fromName-field-empty");
        return;
      } else if (FAQUtils.isFieldEmpty(from)) {
        warning("UISendMailForm.msg.from-field-empty");
        return;
      } else if (!FAQUtils.isValidEmailAddresses(from)) {
        warning("UISendMailForm.msg.invalid-from-field");
        return;
      } else if (FAQUtils.isFieldEmpty(to)) {
        warning("UISendMailForm.msg.to-field-empty");
        return;
      } else if (!FAQUtils.isValidEmailAddresses(to)) {
        warning("UISendMailForm.msg.invalid-to-field");
        return;
      } else if (subject == null || subject.trim().length() < 0) {
        warning("UISendMailForm.msg.subject-field-empty");
        return;
      }
      Message message = new Message();
      message.setMimeType(MIMETYPE_TEXTHTML);
      message.setFrom(fullFrom);
      message.setTo(to);
      message.setSubject(subject);
      message.setBody(body);
      try {
        MailService mailService = (MailService) sendMailForm.getApplicationComponent(MailService.class);
        mailService.sendMessage(message);
      } catch (Exception e) {
        warning("UISendMailForm.msg.send-mail-error");
        sendMailForm.log.error("Can not send email, exception: " + e.getMessage());
        return;
      }
      info("UISendMailForm.msg.send-mail-success", false);
      UIAnswersPortlet portlet = sendMailForm.getAncestorOfType(UIAnswersPortlet.class);
      UIPopupAction popupAction = portlet.getChild(UIPopupAction.class);
      popupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  static public class ToActionListener extends EventListener<UISendMailForm> {
    public void execute(Event<UISendMailForm> event) throws Exception {
      UISendMailForm sendMailForm = event.getSource();
      UIPopupContainer popupContainer = sendMailForm.getAncestorOfType(UIPopupContainer.class);
      UIAddressEmailsForm addressEmailsForm = sendMailForm.openPopup(popupContainer, UIAddressEmailsForm.class, 660, 0);
      addressEmailsForm.setRecipientsType(FILED_TO);
      String toAddressString = ((UIFormStringInput) sendMailForm.getChildById(FILED_TO)).getValue();
      InternetAddress[] toAddresses = FAQUtils.getInternetAddress(toAddressString);
      List<String> emailList = new ArrayList<String>();
      sendMailForm.listAnotherEmail = new ArrayList<String>();
      for (int i = 0; i < toAddresses.length; i++) {
        if (toAddresses[i] != null)
          emailList.add(toAddresses[i].getAddress());
      }
      sendMailForm.listAnotherEmail.addAll(emailList);
      List<User> toUser = sendMailForm.getToUsers();
      if (toUser != null && toUser.size() > 0) {
        List<User> userList = new ArrayList<User>();
        for (User ct : toUser) {
          if (emailList.contains(ct.getEmail())) {
            userList.add(ct);
            sendMailForm.listAnotherEmail.remove(ct.getEmail());
          }
        }
        addressEmailsForm.setAlreadyCheckedUser(userList);
      }
    }
  }

  static public class CancelActionListener extends EventListener<UISendMailForm> {
    public void execute(Event<UISendMailForm> event) throws Exception {
      UISendMailForm sendMailForm = event.getSource();
      UIAnswersPortlet portlet = sendMailForm.getAncestorOfType(UIAnswersPortlet.class);
      UIPopupAction popupAction = portlet.getChild(UIPopupAction.class);
      popupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  static public class ChangeLanguageActionListener extends EventListener<UISendMailForm> {
    public void execute(Event<UISendMailForm> event) throws Exception {
      UISendMailForm sendMailForm = event.getSource();
      UIFormSelectBox formSelectBox = sendMailForm.getChildById(FILED_QUESTION_LANGUAGE);
      UIFormRichtextInput body = sendMailForm.getChildById(FILED_MESSAGE);
      UIFormStringInput subject = sendMailForm.getChildById(FILED_SUBJECT);
      String language = formSelectBox.getValue();
      String contenQuestion = "", qsId = sendMailForm.questionId;
      StringBuilder strBuilder = new StringBuilder();
      for (QuestionLanguage questionLanguage : sendMailForm.listQuestionLanguage) {
        if (questionLanguage.getLanguage().equals(language)) {
          contenQuestion = questionLanguage.getQuestion();
          Answer[] answers = questionLanguage.getAnswers();
          strBuilder.append(sendMailForm.getLabel("change-content")).append(":<p><strong>").append(sendMailForm.getLabel("Question")).append("</strong> ")
                    .append(contenQuestion).append("</p>").append("<p><strong>").append(sendMailForm.getLabel("Detail")).append("</strong> ")
                    .append(questionLanguage.getDetail()).append("</p>");
          if (answers != null && answers.length > 0) {
            strBuilder.append(sendMailForm.getLabel("Response")).append("<p> ").append(answers[sendMailForm.posOfResponse].getResponses()).append("</p>");
          }
          if(!language.equals(sendMailForm.languageIsResponsed)){
            qsId = qsId.concat(UIQuestions.OBJECT_LANGUAGE).concat(language);
            sendMailForm.languageIsResponsed = language;
          }
          strBuilder.append(sendMailForm.getLabel("Link").replaceFirst("<link>", FAQUtils.getQuestionURL(qsId, false)));
          body.setValue(strBuilder.toString());
          subject.setValue(contenQuestion.replaceAll("<br\\s*\\/?>", " "));
          break;
        }
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(sendMailForm);
    }
  }

  public Map<String, String> getServerConfig() {
    return serverConfig_;
  }
}
