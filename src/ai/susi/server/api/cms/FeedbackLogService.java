package ai.susi.server.api.cms;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;

/**
 *  FeedbackLogService
 *  Copyright by Anup Kumar Panwar, @AnupKumarPanwar
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */
import ai.susi.DAO;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * This Endpoint accepts 8 parameters. model,group,language,skill,rating,query,reply,country_name,country_code.
 * rating can be positive or negative
 * before rating a skill the skill must exist in the directory.
 * http://localhost:4000/cms/feedbackLog.json
 */
public class FeedbackLogService
  extends AbstractAPIHandler implements APIHandler {
  private static final long serialVersionUID = 7947060716231250102L;

  @Override
  public UserRole getMinimalUserRole() {
    return UserRole.USER;
  }

  @Override
  public JSONObject getDefaultPermissions(UserRole baseUserRole) {
    return null;
  }

  @Override
  public String getAPIPath() {
    return "/cms/feedbackLog.json";
  }

  @Override
  public ServiceResponse serviceImpl(
    Query call,
    HttpServletResponse response,
    Authorization rights,
    final JsonObjectWithDefault permissions
  )
    throws
      APIException {
    String model_name = call.get("model", "general");
    File model = new File(DAO.model_watch_dir, model_name);
    String group_name = call.get("group", "Knowledge");
    File group = new File(model, group_name);
    String language_name = call.get("language", "en");
    File language = new File(group, language_name);
    String skill_name = call.get("skill", null);
    File skill = SusiSkill.getSkillFileInLanguage(language, skill_name, false);
    String skill_rate = call.get("rating", null);
    String user_query = call.get("user_query", null);
    String susi_reply = call.get("susi_reply", null);
    String country_name = call.get("country_name", null);
    String country_code = call.get("country_code", null);
    String device_type = call.get("device_type", "Others");
    String skill_path = "/susi_skill_data/models/" + model_name + "/" + group_name + "/" + language_name + "/" + skill_name + ".txt";

    JSONObject result = new JSONObject();
    result.put("accepted", false);
    if (!skill.exists()) {
      result.put("message", "skill does not exist");
      return new ServiceResponse(result);
    }
    String idvalue = rights.getIdentity().getName();
    Boolean feedbackExists = false;
    JsonTray skillRating = DAO.feedbackLogs;
    JSONObject modelName = new JSONObject();
    JSONObject groupName = new JSONObject();
    JSONObject languageName = new JSONObject();
    JSONArray skillName = new JSONArray();
    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    if (skillRating.has(model_name)) {
      modelName = skillRating.getJSONObject(model_name);
      if (modelName.has(group_name)) {
        groupName = modelName.getJSONObject(group_name);
        if (groupName.has(language_name)) {
          languageName = groupName.getJSONObject(language_name);
          if (languageName.has(skill_name)) {
            skillName = languageName.getJSONArray(skill_name);

            for (int i = 0; i < skillName.length(); i++) {
              try {
                JSONObject feedbackLogObject = new JSONObject();
                feedbackLogObject = skillName.getJSONObject(i);
                String fb_user_query = feedbackLogObject.get(
                  "user_query"
                ).toString();
                String fb_susi_reply = feedbackLogObject.get(
                  "susi_reply"
                ).toString();
                String fb_country_code = feedbackLogObject.get(
                  "country_code"
                ).toString();
                String fb_device_type = feedbackLogObject.get(
                  "device_type"
                ).toString();

                if (rights.getIdentity().isEmail() && feedbackLogObject.get(
                  "email"
                ).equals(idvalue) || (rights.getIdentity().isUuid(

                ) && feedbackLogObject.get("uuid").equals(idvalue))) {
                  if (fb_user_query.equals(user_query) && fb_susi_reply.equals(
                    susi_reply
                  ) && fb_country_code.equals(
                    country_code
                  ) && fb_device_type.equals(device_type)) {
                    // Feedback once given on an interaction should not change.
                    feedbackExists = true;
                  }
                }
              } catch(Exception e) {
                continue;
              }
            }
          }
        }
      }
    }
    if (!feedbackExists) {
      JSONObject feedbackLogObject = new JSONObject();
      if (rights.getIdentity().isEmail())
      feedbackLogObject.put("email", idvalue);
      if (rights.getIdentity().isUuid())
      feedbackLogObject.put("uuid", idvalue);
      feedbackLogObject.put("timestamp", timestamp);
      feedbackLogObject.put("feedback", skill_rate);
      feedbackLogObject.put("user_query", user_query);
      feedbackLogObject.put("susi_reply", susi_reply);
      feedbackLogObject.put("country_name", country_name);
      feedbackLogObject.put("country_code", country_code);
      feedbackLogObject.put("device_type", device_type);
      feedbackLogObject.put("skill_path", skill_path);
      skillName.put(feedbackLogObject);
    }
    languageName.put(skill_name, skillName);
    groupName.put(language_name, languageName);
    modelName.put(group_name, groupName);
    skillRating.put(model_name, modelName, true);
    result.put("accepted", true);
    result.put("message", "Feedback log updated");
    if (feedbackExists) {
      result.put("message", "Feedback log already exists for same interaction");
    }
    return new ServiceResponse(result);
  }

}

