package com.grammatek.simaromur.network.tiro.pojo;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/*
    GSON adaptions for Response of $ curl -X GET https://tts.tiro.is/v0/voices | jq
      [
        {
          "Gender": "Male",
          "LanguageCode": "is-IS",
          "LanguageName": "Íslenska",
          "SupportedEngines": [
            "standard"
          ],
          "VoiceId": "Other"
        },
        ...
      ]
 */
public class VoiceResponse {
    @SerializedName("Gender")
    public String Gender;
    @SerializedName("LanguageCode")
    public String LanguageCode;
    @SerializedName("LanguageName")
    public String LanguageName;
    @SerializedName("SupportedEngines")
    public List<String> SupportedEngines = null;
    @SerializedName("VoiceId")
    public String VoiceId;
}

