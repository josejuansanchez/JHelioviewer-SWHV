package org.helioviewer.base.math;

public class GL3DQuatd {

    private static final double EPSILON = 0.000001;

    private double a;
    private GL3DVec3d u;

    public static GL3DQuatd createRotation(double angle, GL3DVec3d axis) {
        if (angle == 0.)
            return new GL3DQuatd();

        double halfAngle = angle / 2.0;
        GL3DVec3d axisCopy = axis.copy();
        axisCopy.normalize();
        axisCopy.multiply(Math.sin(halfAngle));
        return new GL3DQuatd(Math.cos(halfAngle), axisCopy);
    }

    public GL3DQuatd(double ax, double ay, double az) {
        ax /= 2.;
        ay /= 2.;
        az /= 2.;
        double sx = Math.sin(ax), cx = Math.cos(ax);
        double sy = Math.sin(ay), cy = Math.cos(ay);
        double sz = Math.sin(az), cz = Math.cos(az);

        this.a = cx * cy * cz + sx * sy * sz;
        this.u = new GL3DVec3d(
                 sx * cy * cz - cx * sy * sz,
                 cx * sy * cz + sx * cy * sz,
                 sx * sy * cz - cx * cy * sz);
    }

    public GL3DQuatd(double ax, double ay) {
        ax /= 2.;
        ay /= 2.;
        double sx = Math.sin(ax), cx = Math.cos(ax);
        double sy = Math.sin(ay), cy = Math.cos(ay);

        this.a = cx * cy;
        this.u = new GL3DVec3d(
                 sx * cy,
                 cx * sy,
                 sx * sy);
    }

    private GL3DQuatd(double a, double x, double y, double z) {
        this(a, new GL3DVec3d(x, y, z));
    }

    private GL3DQuatd(double a, GL3DVec3d u) {
        this.a = a;
        this.u = u;
    }

    public GL3DQuatd() {
        this(1, new GL3DVec3d(0., 0., 0.));
    }

    public void clear() {
        this.a = 1;
        this.u = new GL3DVec3d();
    }

    public GL3DQuatd multiply(GL3DQuatd q) {
        double a1 = this.a;
        double x1 = this.u.x;
        double y1 = this.u.y;
        double z1 = this.u.z;
        double a2 = q.a;
        double x2 = q.u.x;
        double y2 = q.u.y;
        double z2 = q.u.z;

        double a = (a1 * a2 - x1 * x2 - y1 * y2 - z1 * z2);
        double x = (a1 * x2 + x1 * a2 + y1 * z2 - z1 * y2);
        double y = (a1 * y2 - x1 * z2 + y1 * a2 + z1 * x2);
        double z = (a1 * z2 + x1 * y2 - y1 * x2 + z1 * a2);
        GL3DQuatd res = new GL3DQuatd(a, x, y, z);

        return res;
    }

    public GL3DMat4d toMatrix() {
        double w = a, w2 = w * w;
        double x = u.x, x2 = x * x;
        double y = u.y, y2 = y * y;
        double z = u.z, z2 = z * z;

        return new GL3DMat4d(w2 + x2 - y2 - z2, 2 * x * y - 2 * w * z, 2 * x * z + 2 * w * y, 0, 2 * x * y + 2 * w * z, w2 - x2 + y2 - z2, 2 * y * z - 2 * w * x, 0, 2 * x * z - 2 * w * y, 2 * y * z + 2 * w * x, w2 - x2 - y2 + z2, 0, 0, 0, 0, w2 + x2 + y2 + z2);
        /*
         * return new GL3DMat4d( w2+x2-y2-z2, 2*x*y+2*w*z, 2*x*z-2*w*y, 0,
         * 
         * 2*x*y-2*w*z, w2-x2+y2-z2, 2*y*z+2*w*x, 0,
         * 
         * 2*x*z+2*w*y, 2*y*z-2*w*x, w2-x2-y2+z2, 0,
         * 
         * 0, 0, 0, 1 );
         */
    }

    public double getAngle() {
        return this.a;
    }

    public GL3DVec3d getRotationAxis() {
        return this.u;
    }

    public GL3DQuatd add(GL3DQuatd q) {
        this.u.add(q.u);
        this.a += q.a;
        return this;
    }

    public GL3DQuatd subtract(GL3DQuatd q) {
        this.u.subtract(q.u);
        this.a -= q.a;
        return this;
    }

    public GL3DQuatd scale(double s) {
        this.a *= s;
        this.u.multiply(s);
        return this;
    }

    public void rotate(GL3DQuatd q2) {
        GL3DQuatd q1 = this.copy();

        this.a = q1.a * q2.a - q1.u.x * q2.u.x - q1.u.y * q2.u.y - q1.u.z * q2.u.z;
        this.u.x = q1.a * q2.u.x + q1.u.x * q2.a + q1.u.y * q2.u.z - q1.u.z * q2.u.y;
        this.u.y = q1.a * q2.u.y + q1.u.y * q2.a + q1.u.z * q2.u.x - q1.u.x * q2.u.z;
        this.u.z = q1.a * q2.u.z + q1.u.z * q2.a + q1.u.x * q2.u.y - q1.u.y * q2.u.x;

        this.normalize();
    }

    public void rotateWithConjugate(GL3DQuatd q2) {
        GL3DQuatd q1 = this.copy();

        this.a = q1.a * q2.a + q1.u.x * q2.u.x + q1.u.y * q2.u.y + q1.u.z * q2.u.z;
        this.u.x = -q1.a * q2.u.x + q1.u.x * q2.a - q1.u.y * q2.u.z + q1.u.z * q2.u.y;
        this.u.y = -q1.a * q2.u.y + q1.u.y * q2.a - q1.u.z * q2.u.x + q1.u.x * q2.u.z;
        this.u.z = -q1.a * q2.u.z + q1.u.z * q2.a - q1.u.x * q2.u.y + q1.u.y * q2.u.x;

        this.normalize();
    }

    public GL3DQuatd slerp(GL3DQuatd r, double t) {
        double cosAngle = dot(r);

        if (cosAngle > 1 - EPSILON) {
            GL3DQuatd result = r.copy().add(this.copy().subtract(r).scale(t));
            result.normalize();
            return result;
        }

        if (cosAngle < 0)
            cosAngle = 0;
        if (cosAngle > 1)
            cosAngle = 1;

        double theta0 = Math.acos(cosAngle);
        double theta = theta0 * t;
        GL3DQuatd v2 = r.copy().subtract(this.copy().scale(cosAngle));
        v2.normalize();

        GL3DQuatd q = this.copy().scale(Math.cos(theta)).add(v2.scale(Math.sin(theta)));
        q.normalize();
        return q;
    }

    public GL3DQuatd nlerp(GL3DQuatd r, double t) {
        GL3DQuatd result = r.copy().add(this.copy().subtract(r).scale(t));
        result.normalize();
        return result;
    }

    public void set(GL3DQuatd q) {
        this.a = q.a;
        this.u = q.u;
    }

    public GL3DQuatd normalize() {
        double l = Math.sqrt(a * a + u.length2());
        a /= l;
        u.divide(l);
        return this;
    }

    public double dot(GL3DQuatd q) {
        return this.a * q.a + this.u.x * q.u.x + this.u.y * q.u.y + this.u.z * q.u.z;
    }

    public static GL3DQuatd calcRotation(GL3DVec3d startPoint, GL3DVec3d endPoint) {
        GL3DVec3d rotationAxis = GL3DVec3d.cross(startPoint, endPoint);
        double rotationAngle = Math.atan2(rotationAxis.length(), GL3DVec3d.dot(startPoint, endPoint));

        return GL3DQuatd.createRotation(rotationAngle, rotationAxis);
    }

    public GL3DQuatd copy() {
        return new GL3DQuatd(this.a, this.u.copy());
    }

    @Override
    public String toString() {
        return "[" + a + ", " + u.x + ", " + u.y + ", " + u.z + "]";
    }

    public GL3DVec3d rotateVector(GL3DVec3d vec) {
        //q'vq = vec + 2.0 * cross(q.xyz,cross(  q.xyz, vec ) + q.w * vec)
        double vx = vec.z * u.y - vec.y * u.z + a * vec.x;
        double vy = vec.x * u.z - vec.z * u.x + a * vec.y;
        double vz = vec.y * u.x - vec.x * u.y + a * vec.z;
        double vvx = (vz * u.y - vy * u.z) * 2. + vec.x;
        double vvy = (vx * u.z - vz * u.x) * 2. + vec.y;
        double vvz = (vy * u.x - vx * u.y) * 2. + vec.z;
        return new GL3DVec3d(vvx, vvy, vvz);
        //18 mul + 12 add
    }

    public GL3DVec3d rotateInverseVector(GL3DVec3d vec) {
        double vx = -vec.z * u.y + vec.y * u.z + a * vec.x;
        double vy = -vec.x * u.z + vec.z * u.x + a * vec.y;
        double vz = -vec.y * u.x + vec.x * u.y + a * vec.z;
        double vvx = (-vz * u.y + vy * u.z) * 2. + vec.x;
        double vvy = (-vx * u.z + vz * u.x) * 2. + vec.y;
        double vvz = (-vy * u.x + vx * u.y) * 2. + vec.z;
        return new GL3DVec3d(vvx, vvy, vvz);
    }

    public void conjugate() {
        u.x = -u.x;
        u.y = -u.y;
        u.z = -u.z;
    }

    public float[] getFloatArray() {
        return new float[] { (float) u.x, (float) u.y, (float) u.z, (float) a };
    }

}
