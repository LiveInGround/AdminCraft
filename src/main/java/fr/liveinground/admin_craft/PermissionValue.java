package fr.liveinground.admin_craft;

import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionCheck;

public enum PermissionValue {
	ALL(0, Commands.LEVEL_ALL),
	MODERATORS(1, Commands.LEVEL_MODERATORS),
	GAMEMASTERS(2, Commands.LEVEL_GAMEMASTERS),
	ADMINS(3, Commands.LEVEL_ADMINS),
	OWNERS(4, Commands.LEVEL_OWNERS);

	private final int oldValue;
	private final PermissionCheck check;
	private final Permission permission;

	PermissionValue(int oldValue, PermissionCheck check) {
		this.oldValue = oldValue;
		this.check = check;

		if (check instanceof PermissionCheck.Require(Permission permission))
			this.permission = permission;
		else
			permission = null;
	}

	public static PermissionValue fromOld(int oldValue) {
		for (PermissionValue value : values()) {
			if (value.oldValue == oldValue)
				return value;
		}

		throw new IllegalArgumentException("Unknown old value");
	}

	public PermissionCheck check() {
		return check;
	}

	public Permission permission() {
		return permission;
	}
}

