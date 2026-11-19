bool annotationBoxContains(vec3 point, vec3 inner, vec3 outer, float tolerance) {
    vec3 absolute = abs(point);
    vec3 bands = step(inner - vec3(tolerance), absolute);
    return all(lessThanEqual(absolute, outer + vec3(tolerance))) && bands.x + bands.y + bands.z >= 2.0;
}

bool annotationBoxNearestSurface(vec3 position, vec3 ray, vec3 halfSize, float halfThickness) {
    vec3 direction = normalize(ray);
    vec3 inverseDirection = vec3(
        abs(direction.x) > 0.000001 ? 1.0 / direction.x : 1.0e20,
        abs(direction.y) > 0.000001 ? 1.0 / direction.y : 1.0e20,
        abs(direction.z) > 0.000001 ? 1.0 / direction.z : 1.0e20);
    float travel = max(1.0, 4.0 * (halfSize.x + halfSize.y + halfSize.z + halfThickness));
    vec3 origin = position - direction * travel;
    vec3 outer = halfSize + vec3(halfThickness);
    vec3 inner = max(halfSize - vec3(halfThickness), vec3(0.0));
    vec3 a = (-outer - origin) * inverseDirection;
    vec3 b = (outer - origin) * inverseDirection;
    vec3 nearPoint = min(a, b);
    vec3 farPoint = max(a, b);
    float entry = max(nearPoint.x, max(nearPoint.y, nearPoint.z));
    float exitPoint = min(farPoint.x, min(farPoint.y, farPoint.z));
    float tolerance = max(0.000002, travel * 0.000002);
    if (annotationBoxContains(origin + direction * entry, inner, outer, tolerance)) {
        return abs(entry - travel) <= tolerance;
    }
    float nearest = 1.0e20;
    for (int axis = 0; axis < 3; axis++) {
        for (int side = 0; side < 2; side++) {
            float coordinate = axis == 0 ? origin.x : (axis == 1 ? origin.y : origin.z);
            float inverse = axis == 0 ? inverseDirection.x : (axis == 1 ? inverseDirection.y : inverseDirection.z);
            float plane = axis == 0 ? inner.x : (axis == 1 ? inner.y : inner.z);
            float candidate = ((side == 0 ? -plane : plane) - coordinate) * inverse;
            if (candidate >= entry - tolerance && candidate <= exitPoint + tolerance
                && annotationBoxContains(origin + direction * candidate, inner, outer, tolerance)) {
                nearest = min(nearest, candidate);
            }
        }
    }
    return abs(nearest - travel) <= tolerance;
}
