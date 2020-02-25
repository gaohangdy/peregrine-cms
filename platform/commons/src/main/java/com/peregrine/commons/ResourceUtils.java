package com.peregrine.commons;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.HashMap;
import java.util.Map;

import static com.peregrine.commons.util.PerConstants.JCR_PRIMARY_TYPE;
import static com.peregrine.commons.util.PerConstants.SLASH;
import static com.peregrine.commons.Strings.COLON;
import static com.peregrine.commons.Strings._SCORE;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class ResourceUtils {

    private ResourceUtils() {
        throw new UnsupportedOperationException();
    }

    public static Resource getFirstExistingAncestorOnPath(final ResourceResolver resourceResolver, final String path) {
        String existingPath = path;
        Resource resource = null;
        while (StringUtils.isNotBlank(existingPath) &&
                (resource = resourceResolver.getResource(existingPath)) == null) {
            existingPath = StringUtils.substringBeforeLast(existingPath, SLASH);
        }

        if (isNull(resource)) {
            return resourceResolver.getResource(SLASH);
        }

        return resource;
    }

    public static Resource getOrCreateResource(
            final ResourceResolver resourceResolver,
            final String path,
            final String resourceTypes)
            throws PersistenceException {
        Resource resource = getFirstExistingAncestorOnPath(resourceResolver, path);
        final String missingPath;
        if (isNull(resource)) {
            missingPath = path;
        } else {
            missingPath = StringUtils.substringAfter(path, resource.getPath());
        }

        final String[] missing = StringUtils.split(missingPath, SLASH);
        final Map<String, Object> properties = new HashMap<>();
        properties.put(JCR_PRIMARY_TYPE, resourceTypes);
        for (final String name : missing) {
            resource = resourceResolver.create(resource, name, properties);
        }

        return resourceResolver.getResource(path);
    }

    public static String fileNameToJcrName(final String name) {
        if (!startsWith(name, _SCORE)) {
            return name;
        }

        final String nameAfterUnderscore = name.substring(1);
        if (!contains(nameAfterUnderscore, _SCORE) || startsWith(nameAfterUnderscore, _SCORE)) {
            return name;
        }

        final String prefix = substringBefore(nameAfterUnderscore, _SCORE);
        final String suffix = substringAfter(nameAfterUnderscore, _SCORE);
        if (isNotBlank(suffix)) {
            return prefix + COLON + suffix;
        }

        return name;
    }

    public static String jcrNameToFileName(final String name) {
        if (contains(name, COLON)) {
            final String prefix = substringBefore(name, COLON);
            final String suffix = substringAfter(name, COLON);
            return _SCORE + prefix + _SCORE + suffix;
        }

        return name;
    }

    public static String jcrPathToFilePath(final String path) {
        if (startsWith(path, SLASH)) {
            return SLASH + jcrPathToFilePath(path.substring(1));
        }

        if (endsWith(path, SLASH)) {
            return jcrPathToFilePath(path.substring(0, path.length() - 1)) + SLASH;
        }

        if (isBlank(path)) {
            return path;
        }

        final StringBuilder result = new StringBuilder();
        for (final String name : path.split(SLASH)) {
            result.append(SLASH);
            result.append(jcrNameToFileName(name));
        }

        return result.substring(1);
    }

}