package custom.rapidoidx.db.model;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoidx.db.DbColumn;
import org.rapidoidx.db.RichEntity;

/*
 * #%L
 * rapidoid-x-db-tests
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

@Authors("Nikolche Mihajlovski")
@Since("3.0.0")
public interface ICompany extends RichEntity {

	DbColumn<String> name();

	DbColumn<IProfile> profile();

}
