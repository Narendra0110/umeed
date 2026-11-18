package com.umeed.users;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/** Each user's app language, "en" or "hi" (F07). Implemented by UserService. */
public interface UserLanguages {

    /** The language of each known user. Unknown ids are left out. */
    Map<UUID, String> languagesByIds(Collection<UUID> userIds);
}
