package info;

public class User
{
    public String id;
    public String name;

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if ((o == null) || (getClass() != o.getClass())) return false;

        User user = (User)o;

        if (this.id != null ? !this.id.equals(user.id) : user.id != null) return false;
        if (this.name != null ? !this.name.equals(user.name) : user.name != null) return false;

        return true;
    }

    public int hashCode()
    {
        int result = this.id != null ? this.id.hashCode() : 0;
        result = 31 * result + (this.name != null ? this.name.hashCode() : 0);
        return result;
    }

    public String toString()
    {
        return "User{id='" + this.id + '\'' + ", name='" + this.name + '\'' + '}';
    }
}

/* Location:           C:\MAWalkerMod\MAWalkerMod\
 * Qualified Name:     info.User
 * JD-Core Version:    0.6.2
 */