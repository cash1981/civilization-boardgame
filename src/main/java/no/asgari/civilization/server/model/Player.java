/*
 * Copyright (c) 2015 Shervin Asgari
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package no.asgari.civilization.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Data
@JsonRootName("player")
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {
    @JsonIgnore
    public static final String COL_NAME = "player";
    @JsonIgnore
    public static final String USERNAME = "username";
    @JsonIgnore
    public static final String EMAIL = "email";

    @Id
    private String id;

    @NotBlank
    //Unique
    @Indexed(unique = true)
    private String username;

    @Email
    @Indexed(unique = true)
    private String email;

    private boolean disableEmail = false;

    @NotBlank
    private String password;

    private String newPassword;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime emailSent;

    /**
     * Set of unique active games This may be reduntant as it can be calculated by looping through all pbfs.players and finding match.
     */
    private Set<String> gameIds = new HashSet<>();

    @JsonIgnore
    public Optional<LocalDateTime> getIfEmailSent() {
        return Optional.ofNullable(emailSent);
    }
}
