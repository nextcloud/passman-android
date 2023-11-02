package es.wolfi.passman.API;

public class SharingACL {
    public enum PERMISSION {
        READ(0x01),
        WRITE(0x02),
        FILES(0x04),
        HISTORY(0x08),
        OWNER(0x80);

        final int permissionValue;

        PERMISSION(int value) {
            permissionValue = value;
        }

        public int permissionValue() {
            return permissionValue;
        }
    }

    private int permission;

    public SharingACL(int permission) {
        this.permission = permission;
    }

    /**
     * Checks if a user has the given permission/s.
     */
    public boolean hasPermission(PERMISSION permission) {
        return permission.permissionValue == (this.permission & permission.permissionValue);
    }

    /**
     * Adds a permission to a user, leaving any other permissions intact.
     */
    public void addPermission(PERMISSION permission) {
        this.permission = this.permission | permission.permissionValue;
    }

    /**
     * Removes a given permission from the item, leaving any other intact.
     */
    public void removePermission(PERMISSION permission) {
        this.permission = this.permission & ~permission.permissionValue;
    }

    public int getPermission() {
        return this.permission;
    }
}
